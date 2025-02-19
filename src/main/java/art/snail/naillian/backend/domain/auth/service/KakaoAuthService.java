package art.snail.naillian.backend.domain.auth.service;

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
import java.util.HashMap;
import java.util.Map;

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

    public Mono<Map<String, Object>> kakaoLogin(Map<String, String> body) {
        // 여기서 body 검증
        if (body == null || !body.containsKey("code")) {
            // 실패 응답 Map 만들어 반환
            return Mono.just(buildErrorResponse(400, "카카오 인가코드(code)가 필요합니다."));
        }

        String code = body.get("code");

        // 이제 아래 로직들은 “인가코드 -> 액세스 토큰 -> 사용자 정보 -> 유저 생성/조회 -> 토큰 발급 -> 응답”
        return getAccessTokenJson(code)
                .flatMap(this::extractAccessToken)
                .flatMap(this::fetchKakaoUserInfo)
                .flatMap(this::findOrCreateUser)
                .map(this::issueTokenAndBuildSuccess)
                .onErrorResume(e -> {
                    // ReportableError라면 그대로 code, message 뽑아서 에러 응답 만듦
                    if (e instanceof ReportableError) {
                        ReportableError re = (ReportableError) e;
                        return Mono.just(buildErrorResponse(re.getStatus().value(), re.getMessage()));
                    }
                    // 예상치 못한 예외 -> 500
                    return Mono.just(buildErrorResponse(500, "서버 오류 발생: " + e.getMessage()));
                });
    }

    /**
     * 1) 인가 코드로 카카오 액세스 토큰(JSON) 얻기
     */

    private Mono<JsonNode> getAccessTokenJson(String code) {

        return webClient.post()
                .uri(TOKEN_URI)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", restApiKey)
                        .with("redirect_uri", callbackUrl)
                        .with("code", code)
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
                                    // 탈퇴된 계정이면 접근 차단 등 처리
                                    if (existingUser.getDeletedAt() != null) {
                                        return Mono.error(new ReportableError(HttpStatus.FORBIDDEN, "이미 탈퇴 처리된 사용자입니다."));
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

    private Map<String, Object> issueTokenAndBuildSuccess(User user){
        Date now = new Date();
        String accessToken = jwtProvider.generateAccessToken(user.getId(), now);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), now);

        tokenService.storeTokenPair(accessToken, refreshToken, user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "카카오 로그인 성공");
        response.put("data", data);

        return response;
    }

    // 에러 응답 바디
    private Map<String, Object> buildErrorResponse(int code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        return body;
    }
}
