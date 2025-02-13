package art.snail.naillian.backend.domain.auth.service;

import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.user.entity.SocialLogin;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.entity.UserType;
import art.snail.naillian.backend.domain.user.repository.SocialLoginRepository;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {


    private static final Logger log = LoggerFactory.getLogger(KakaoAuthService.class);

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final JwtProvider jwtProvider;

    @Value("${kakao.auth.rest-api-key}")
    private String restApiKey;

    @Value("${kakao.auth.callback-url}")
    private String callbackUrl;

    @Value("${kakao.auth.client-secret:}")
    private String clientSecret;


    private final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    /** 1. 카카오 액세스 토큰 요청 */

    public Mono<String> getAccessToken(String code) {
        log.info("[log] 카카오 토큰 요청 시작 - 받은 코드: {}", code);
        log.info("[log] 클라이언트 ID: {}", restApiKey);
        log.info("[log] 리디렉트 URI: {}", callbackUrl);

        WebClient.RequestHeadersSpec<?> request = webClient.post()
                .uri(TOKEN_URI)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", restApiKey)
                        .with("redirect_uri", callbackUrl)
                        .with("code", code)
                        .with("client_secret", clientSecret)
                );

        return request.exchangeToMono(response -> {
                    log.info("[log] 카카오 응답 상태 코드: {}", response.statusCode());
                    return response.bodyToMono(String.class);
                })
                .doOnSuccess(response -> log.info("[log] 카카오 응답 본문: {}", response))
                .doOnError(error -> log.error("[log] 카카오 API 요청 실패: {}", error.getMessage()));
    }

    /** 사용자 정보 조회 후 DB 저장 */
    public Mono<User> getUserInfo(String accessTokenJson) {
        log.info("[log]사용자 정보 조회 요청 - AccessToken JSON: {}", accessTokenJson);

        return Mono.fromCallable(() -> new ObjectMapper().readTree(accessTokenJson))
                .onErrorResume(e -> {
                    log.error("[log]AccessToken JSON 파싱 오류", e);
                    return Mono.error(new RuntimeException("[log]AccessToken JSON 파싱 오류", e));
                })
                .flatMap(tokenNode -> {
                    if (!tokenNode.has("access_token") || !tokenNode.get("access_token").isTextual()) {
                        log.error("[log]AccessToken JSON에 'access_token' 키가 없거나 잘못된 형식입니다: {}", tokenNode);
                        return Mono.error(new RuntimeException("[log]올바른 'access_token' 값을 찾을 수 없습니다."));
                    }

                    String accessToken = tokenNode.get("access_token").asText();
                    log.info("[log]실제 API 요청에 사용될 AccessToken: {}", accessToken);

                    return webClient.get()
                            .uri(USER_INFO_URI)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .doOnSuccess(jsonNode -> log.info("[log]카카오 사용자 정보 응답: {}", jsonNode))
                            .flatMap(jsonNode -> {
                                if (!jsonNode.has("id") || !jsonNode.get("id").isTextual()) {
                                    log.error("[log]카카오 응답에 사용자 ID 없음: {}", jsonNode);
                                    return Mono.error(new RuntimeException("[log]카카오 응답에서 사용자 정보를 찾을 수 없습니다."));
                                }

                                String platformUserId = jsonNode.get("id").asText();
                                String nickname = jsonNode.path("properties").path("nickname").asText();
                                log.info("[log]카카오 사용자 ID: {}, 닉네임: {}", platformUserId, nickname);

                                return socialLoginRepository.findByPlatformUserId(platformUserId)
                                        .flatMap(socialLogin -> userRepository.findById(socialLogin.getUserId()))
                                        .switchIfEmpty(createNewUser(platformUserId, nickname));
                            })
                            .onErrorResume(e -> {
                                log.error("[log]사용자 정보 조회 중 오류 발생", e);
                                return Mono.error(new RuntimeException("[log]사용자 정보 조회 실패", e));
                            });
                });
    }




    /** 새로운 사용자 생성 */
    private Mono<User> createNewUser(String platformUserId, String nickname) {
        return userRepository.save(
                        User.builder()
                                .nickname(nickname)
                                .userType(UserType.CUSTOMER)
                                .registeredIp("UNKNOWN")
                                .build()
                )
                .flatMap(user -> socialLoginRepository.save(
                        SocialLogin.builder()
                                .userId(user.getId())
                                .platform("KAKAO")
                                .platformUserId(platformUserId)
                                .build()
                ).thenReturn(user));
    }

    public Mono<Map<String, String>> generateJwtTokens(User user) {
        return Mono.just(Map.of(
                "accessToken", jwtProvider.generateAccessToken(user.getId()),
                "refreshToken", jwtProvider.generateRefreshToken(user.getId())
        ));
    }
}
