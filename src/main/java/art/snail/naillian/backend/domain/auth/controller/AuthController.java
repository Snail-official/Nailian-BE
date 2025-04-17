package art.snail.naillian.backend.domain.auth.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.auth.dto.KakaoAuthRequest;
import art.snail.naillian.backend.domain.auth.dto.UserTokenPairDTO;
import art.snail.naillian.backend.domain.auth.service.AuthenticationService;
import art.snail.naillian.backend.domain.auth.service.KakaoAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final KakaoAuthService kakaoAuthService;
    private final AuthenticationService authenticationService;

    @PostMapping("/kakao")
    public Mono<CommonResponse<UserTokenPairDTO>> kakaoLogin(@RequestBody KakaoAuthRequest request) {
        return kakaoAuthService.handleLoginByKakaoAccessToken(request.getKakaoAccessToken())
                .map(dto -> CommonResponse.success(dto, "카카오 로그인 성공"));
    }

    /** Token 재발급 */
    @PostMapping("/reissue")
    public Mono<CommonResponse<String>> reIssueToken(@RequestBody Map<String, String> requestBody) {
        String refreshToken = requestBody.get("refreshToken");
        return authenticationService.reIssueAccessToken(refreshToken)
                .map(token -> CommonResponse.success(token, "토큰 재발급 성공"));
    }

    @PostMapping("/logout")
    public Mono<CommonResponse<Void>> logout(@RequestHeader("Authorization") String accessToken) {
        return authenticationService.logout(accessToken)
                .map(value -> CommonResponse.success(null, "로그아웃 성공"));
    }
}
