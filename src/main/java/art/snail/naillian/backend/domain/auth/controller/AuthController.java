package art.snail.naillian.backend.domain.auth.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.auth.dto.KakaoAuthRequest;
import art.snail.naillian.backend.domain.auth.dto.UserTokenPairDTO;
import art.snail.naillian.backend.domain.auth.service.AuthenticationService;
import art.snail.naillian.backend.domain.auth.service.KakaoAuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(KakaoAuthService.class);


    private final KakaoAuthService kakaoAuthService;
    private final AuthenticationService authenticationService;

    @PostMapping("/kakao")
    public Mono<CommonResponse<UserTokenPairDTO>> kakaoLogin(@RequestBody KakaoAuthRequest request) {
        return kakaoAuthService.handleLoginByKakaoAccessToken(request.getKakaoAccessToken())
                .map(dto -> CommonResponse.success(dto, "카카오 로그인 성공"));
    }

    /** Token 재발급 */
    @PostMapping("/reissue")
    public Mono<ResponseEntity<Map<String, Object>>> reIssueToken(@RequestBody Map<String, String> requestBody) {
        String refreshToken = requestBody.get("refreshToken");
        return authenticationService.reIssueAccessToken(refreshToken)
                .map(newAccessToken -> ResponseEntity.ok(Map.of("accessToken", newAccessToken)));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, Object>>> logout(@RequestHeader("Authorization") String accessToken) {
        return authenticationService.logout(accessToken)
                .map(body -> ResponseEntity.ok(body));
    }


}