package art.snail.naillian.backend.domain.auth.service;

import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.user.entity.SocialLogin;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.repository.SocialLoginRepository;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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

    private final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    /** 1. 카카오 액세스 토큰 요청 */

    public Mono<String> getAccessToken(String code) {
        log.info("🔍 카카오 토큰 요청: code={}", code);

        return webClient.post()
                .uri(TOKEN_URI)
                .header("Content-Type", "application/x-www-form-urlencoded") // ✅ 헤더 추가
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", restApiKey)
                        .with("redirect_uri", callbackUrl)
                        .with("code", code))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(jsonNode -> log.info("카카오 API 응답: {}", jsonNode))
                .map(jsonNode -> jsonNode.get("access_token").asText())
                .doOnSuccess(token -> log.info("카카오 API AccessToken 받음: {}", token))
                .doOnError(error -> log.error("카카오 API 요청 실패: {}", error.getMessage()));
    }



    /** 사용자 정보 조회 후 DB 저장 */
    public Mono<User> getUserInfo(String accessToken) {
        log.info("받은 카카오 AccessToken: {}", accessToken);

        return webClient.get()
                .uri(USER_INFO_URI)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(jsonNode -> log.info("카카오 사용자 정보 응답: {}", jsonNode)) // 응답 로그 추가
                .flatMap(jsonNode -> {
                    if (!jsonNode.has("id")) {
                        log.error("카카오 응답에 사용자 ID 없음: {}", jsonNode);
                        return Mono.error(new RuntimeException("카카오 응답에서 사용자 정보를 찾을 수 없습니다."));
                    }

                    String platformUserId = jsonNode.get("id").asText();
                    String nickname = jsonNode.path("properties").path("nickname").asText();
                    log.info("카카오 사용자 ID: {}, 닉네임: {}", platformUserId, nickname);

                    return socialLoginRepository.findByPlatformUserId(platformUserId)
                            .flatMap(socialLogin -> userRepository.findById(socialLogin.getUserId()))
                            .switchIfEmpty(createNewUser(platformUserId, nickname));
                })
                .onErrorResume(e -> {
                    log.error("사용자 정보 조회 중 오류 발생", e);
                    return Mono.error(new RuntimeException("사용자 정보 조회 실패", e));
                });
    }


    /** 새로운 사용자 생성 */
    private Mono<User> createNewUser(String platformUserId, String nickname) {
        return userRepository.save(
                        User.builder()
                                .nickname(nickname)
                                .userType("KAKAO")
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
