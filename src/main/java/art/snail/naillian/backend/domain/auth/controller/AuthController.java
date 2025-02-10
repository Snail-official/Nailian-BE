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
    private final UserService userService;
    private final AuthenticationService authenticationService;

    /** 1. 카카오 로그인(GET /kakao/login/{code}) */
    @GetMapping("/kakao/login/{code}")
    public Mono<ResponseEntity<Map<String, Object>>> kakaoLogin(@PathVariable String code) {
        log.info("카카오 로그인 API 호출 - 받은 코드: {}", code);

        return kakaoAuthService.getAccessToken(code) // 1️⃣ 카카오 액세스 토큰 요청
                .flatMap(kakaoAuthService::getUserInfo) // 2️⃣ 사용자 정보 조회 후 회원가입 또는 로그인
                .map(tokens -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("code", 200);
                    response.put("message", "로그인 성공");
                    response.put("accessToken", tokens.get("accessToken"));
                    response.put("refreshToken", tokens.get("refreshToken"));
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("code", 400);
                    errorResponse.put("message", "카카오 로그인 실패: " + error.getMessage());
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

    @PostMapping("/signUp")
    public Mono<ResponseEntity<Map<String, Object>>> signUp(@RequestBody Map<String, String> requestBody){
        String authorizationCode = requestBody.get("authorizationcode");
        if(authorizationCode == null || authorizationCode.isEmpty()){
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "authorization 값이 필요합니다."
            )));
        }

        return kakaoAuthService.getAccessToken(authorizationCode)
                .flatMap(kakaoAuthService::getUserInfo)
                .map(tokens -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("code", 200);
                    response.put("message", "가입 성공");
                    response.put("accessToken", tokens.get("accessToken"));
                    response.put("refreshToken", tokens.get("refreshToken"));
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> Mono.just(ResponseEntity.badRequest().body(Map.of(
                        "code", 400,
                        "message", "중복확인을 다시 해주세요: " + error.getMessage()
                ))));
    }

    /** Token 재발급 */
    @PostMapping("/re-issue")
    public Mono<ResponseEntity<Map<String, Object>>> reIssueToken(@RequestBody Map<String, String> requestBody) {
        String refreshToken = requestBody.get("refreshToken");
        if (refreshToken == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "refreshToken 값이 필요합니다."
            )));
        }
        return authenticationService.reIssueAccessToken(refreshToken)
                .map(newAccessToken -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("code", 200);
                    response.put("message", "토큰 갱신 성공");
                    response.put("accessToken", newAccessToken);
                    return ResponseEntity.ok(response);
                })
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest().body(Map.of(
                        "code", 400,
                        "message", "유효하지 않은 토큰입니다."
                ))));
    }

}