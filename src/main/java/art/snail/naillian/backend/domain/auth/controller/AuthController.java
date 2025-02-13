package art.snail.naillian.backend.domain.auth.controller;

import art.snail.naillian.backend.domain.auth.service.AuthenticationService;
import art.snail.naillian.backend.domain.auth.service.KakaoAuthService;
import art.snail.naillian.backend.domain.user.service.UserService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final AuthenticationService authenticationService;

    /** 카카오 로그인 */
    @GetMapping("/kakao/login/{code}")
    public Mono<ResponseEntity<Map<String, String>>> kakaoLogin(@PathVariable String code) {
        return kakaoAuthService.getAccessToken(code)
                .flatMap(kakaoAuthService::getUserInfo)
                .map(ResponseEntity::ok);
    }

    /** 콜백 URL 테스트 */
    @GetMapping("/kakao")
    public Mono<ResponseEntity<String>> handleCallback(@RequestParam(name = "code", required = false) String code) {
        if (code == null) {
            log.error("카카오에서 code가 전달되지 않음");
            return Mono.just(ResponseEntity.badRequest().body("카카오에서 code가 전달되지 않았습니다."));
        }
        log.info("카카오 인증 코드: {}", code);
        return Mono.just(ResponseEntity.ok("Received code: " + code));
    }

    @PostMapping("/signUp")
    public Mono<ResponseEntity<Map<String, String>>> signUp(@RequestBody Map<String, String> requestBody) {
        String authorizationCode = requestBody.get("authorizationcode");
        return authenticationService.signUpWithKakao(authorizationCode)
                .map(response -> ResponseEntity.ok(response));
    }

    /** Token 재발급 */
    @PostMapping("/re-issue")
    public Mono<ResponseEntity<Map<String, Object>>> reIssueToken(@RequestBody Map<String, String> requestBody) {
        String refreshToken = requestBody.get("refreshToken");
        return authenticationService.reIssueAccessToken(refreshToken)
                .map(newAccessToken -> ResponseEntity.ok(Map.of("accessToken", newAccessToken)));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, Object>>> logout(@RequestHeader("Authorization") String accessToken) {
        return authenticationService.logout(accessToken)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

}