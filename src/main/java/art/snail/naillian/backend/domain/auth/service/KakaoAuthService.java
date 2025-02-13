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

/** 카카오 API 처리만 담당 */


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

    /** 카카오 액세스 토큰 요청 */

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
    public Mono<Map<String, String>> getUserInfo(String accessTokenJson) {
        return Mono.fromCallable(() -> new ObjectMapper().readTree(accessTokenJson))
                .flatMap(tokenNode -> {
                    String accessToken = tokenNode.get("access_token").asText();
                    return webClient.get()
                            .uri(USER_INFO_URI)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .flatMap(jsonNode -> {
                                if (!jsonNode.has("id")) {
                                    return Mono.error(new RuntimeException("카카오 응답에서 사용자 정보를 찾을 수 없습니다."));
                                }
                                String platformUserId = String.valueOf(jsonNode.get("id").asLong());
                                String nickname = jsonNode.path("properties").path("nickname").asText();
                                String email = jsonNode.path("kakao_account").path("email").asText();

                                return Mono.just(Map.of(
                                        "platformUserId", platformUserId,
                                        "nickname", nickname,
                                        "email", email
                                ));
                            });
                });
    }

    /** 사용자가 이미 가입된 회원인지 확인 */
    public Mono<User> findUserByKakaoId(String platformUserId){
        return socialLoginRepository.findByPlatformUserId(platformUserId)
                .flatMap(socialLogin -> userRepository.findById(socialLogin.getUserId()));

    }
}
