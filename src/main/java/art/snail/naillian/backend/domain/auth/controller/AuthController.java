package art.snail.naillian.backend.domain.auth.controller;

import art.snail.naillian.backend.domain.auth.service.KakaoAuthService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(KakaoAuthService.class);


    private final KakaoAuthService kakaoAuthService;

    /** 1. 카카오 로그인(GET /kakao/login/{code}) */
    @GetMapping("/kakao/login/{code}")
    public Mono<ResponseEntity<Map<String, Object>>> kakaoLogin(@PathVariable String code) {
        log.info("카카오 로그인 API 호출 - 받은 코드: {}", code);

        return kakaoAuthService.getAccessToken(code)
                .flatMap(token -> {
                    log.info("받은 AccessToken: {}", token);
                    return kakaoAuthService.getUserInfo(token);
                })
                .flatMap(user -> kakaoAuthService.generateJwtTokens(user)
                        .map(tokens -> {
                            log.info("JWT 토큰 발급 완료 - 닉네임: {}, AccessToken: {}, RefreshToken: {}",
                                    user.getNickname(), tokens.get("accessToken"), tokens.get("refreshToken"));

                            Map<String, Object> responseBody = Map.of(
                                    "nickname", user.getNickname(),
                                    "accessToken", tokens.get("accessToken"),
                                    "refreshToken", tokens.get("refreshToken")
                            );

                            return ResponseEntity.ok(responseBody);
                        }))
                .onErrorResume(error -> {
                    log.error("카카오 로그인 API 오류 발생", error);

                    Map<String, Object> errorResponse = Map.of(
                            "error", "카카오 로그인 실패",
                            "message", error.getMessage()
                    );

                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }

    @GetMapping("/kakao")
    public Mono<ResponseEntity<String>> handleCallback(@RequestParam(name = "code", required = false) String code) {
        if (code == null) {
            log.error("카카오에서 code가 전달되지 않음");
            return Mono.just(ResponseEntity.badRequest().body("카카오에서 code가 전달되지 않았습니다."));
        }
        log.info("카카오 인증 코드: {}", code);
        return Mono.just(ResponseEntity.ok("Received code: " + code));
    }

}