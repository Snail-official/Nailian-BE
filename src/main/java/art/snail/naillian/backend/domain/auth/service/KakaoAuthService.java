package art.snail.naillian.backend.domain.auth.service;

import art.snail.naillian.backend.domain.auth.dto.UserTokenPairDTO;
import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.user.entity.SocialLogin;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.entity.UserType;
import art.snail.naillian.backend.domain.user.repository.SocialLoginRepository;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import art.snail.naillian.backend.errors.ReportableError;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Date;

/** 카카오 API 처리만 담당 */


@Service
@RequiredArgsConstructor
public class KakaoAuthService {


    private static final Logger log = LoggerFactory.getLogger(KakaoAuthService.class);

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final TokenService tokenService;
    private final JwtProvider jwtProvider;

    @Value("${kakao.auth.rest-api-key}")
    private String restApiKey;

    @Value("${kakao.auth.callback-url}")
    private String callbackUrl;

    @Value("${kakao.auth.client-secret:}")
    private String clientSecret;


    private final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    /**
     * 1) AuthorizationCode -> 카카오 액세스 토큰 발급
     * 2) 액세스 토큰으로 카카오 사용자 정보 조회
     * 3) DB 조회 -> 기존 유저면 User 리턴, 없으면 새로운 User 생성
     * 4) JWT 발급 + Redis 저장
     * 5) "code, message, data" 형태의 Map 응답
     */

    public Mono<UserTokenPairDTO> handleLoginByKakaoAccessToken(String kakaoToken) {
        if (kakaoToken == null || kakaoToken.isEmpty())
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "카카오 액세스 토큰이 필요합니다."));

        // 이제 아래 로직들은 “인가코드 -> 액세스 토큰 -> 사용자 정보 -> 유저 생성/조회 -> 토큰 발급 -> 응답”
        return Mono.just(kakaoToken)
                .flatMap(this::fetchKakaoUserInfo)
                .flatMap(this::findOrCreateUser)
                .flatMap(this::issueTokenAndBuildSuccess);
    }

    /**
     * 1) 인가 코드로 카카오 액세스 토큰(JSON) 얻기
     */

    private Mono<JsonNode> getAccessTokenJson(String kakaoCode) {

        return webClient.post()
                .uri(TOKEN_URI)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", restApiKey)
                        .with("redirect_uri", callbackUrl)
                        .with("code", kakaoCode)
                        .with("client_secret", clientSecret)
                )
                .retrieve()
                // String이 아닌 JsonNode로 바로 받으면 has, get 사용 가능
                .bodyToMono(JsonNode.class)
                .onErrorMap(e -> {
                    log.error("[KakaoAuthService] 카카오 토큰 요청 실패: {}", e.getMessage());
                    return new ReportableError(HttpStatus.BAD_REQUEST, "카카오 토큰 요청 실패: " + e.getMessage());
                });
    }

    /**
     * 2) 액세스 토큰 JSON에서 "access_token" 필드만 추출
     */

    private Mono<String> extractAccessToken(JsonNode tokenJson) {
        if (!tokenJson.has("access_token")) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "카카오 응답에 access_token이 없습니다."));
        }
        return Mono.just(tokenJson.get("access_token").asText());
    }

    /**
     * 3) 액세스 토큰으로 카카오 사용자 정보 조회
     */

    private Mono<JsonNode> fetchKakaoUserInfo(String accessToken) {
        return webClient.get()
                .uri(USER_INFO_URI)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorMap(e -> new ReportableError(HttpStatus.BAD_REQUEST, "카카오 사용자 정보 요청 실패: " + e.getMessage()));
    }

    /**
     * Step 3) DB에서 (platformUserId) 유저 조회 -> 있으면 반환, 없으면 새 User 생성
     */
    private Mono<User> findOrCreateUser(JsonNode userJson) {
        if (!userJson.has("id")) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "카카오 사용자 정보에 id가 없습니다."));
        }

        String platformUserId = String.valueOf(userJson.get("id").asLong());
        String nickname = userJson.path("properties").path("nickname").asText("카카오유저");
        String email = userJson.path("kakao_account").path("email").asText("unknown@kakao.com");

        // 소셜로그인 레코드 검색
        return socialLoginRepository.findByPlatformUserId(platformUserId)
                .flatMap(socialLogin ->
                        userRepository.findById(socialLogin.getUserId())
                                .flatMap(existingUser -> {
                                    // 이미 탈퇴한 사용자를 다시 활성화(재가입 허용)
                                    if (existingUser.getDeletedAt() != null) {
                                        existingUser.setDeletedAt(null);
                                        return userRepository.save(existingUser);
                                    }
                                    return Mono.just(existingUser);
                                })
                )
                // 없으면 새로 생성
                .switchIfEmpty(createNewUser(platformUserId, nickname));
    }
    private Mono<User> createNewUser(String platformUserId, String nickname) {
        User newUser = User.builder()
                .nickname(nickname)
                .userType(UserType.CUSTOMER)
                .registeredIp("UNKNOWN")
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(newUser)
                .flatMap(savedUser -> {
                    SocialLogin sl = SocialLogin.builder()
                            .userId(savedUser.getId())
                            .platform("KAKAO")
                            .platformUserId(platformUserId)
                            .build();
                    return socialLoginRepository.save(sl)
                            .thenReturn(savedUser);
                });
    }

    /**
     * 4) JWT 발급 + Redis 저장
     */

    private Mono<UserTokenPairDTO> issueTokenAndBuildSuccess(User user) {
        Date now = new Date();
        String accessToken = jwtProvider.generateAccessToken(user.getId(), now);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), now);

        tokenService.storeTokenPair(accessToken, refreshToken, user.getId());
        return Mono.just(new UserTokenPairDTO(accessToken, refreshToken));
    }
}
