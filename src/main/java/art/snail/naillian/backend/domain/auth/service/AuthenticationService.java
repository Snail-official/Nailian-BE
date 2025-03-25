package art.snail.naillian.backend.domain.auth.service;

import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** 회원가입 + JWT 토큰 발급 담당 */

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    /** Access Token 검증 및 사용자 ID 추출 */
    public Mono<Integer> extractUserIdFromToken(String accessToken) {
        return Mono.justOrEmpty(accessToken)
                .flatMap(jwtProvider::getUserIdFromToken)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED ,"유효하지 않은 인증 정보입니다.")));
    }

    /** AccessToken을 통해 사용자 조회 */
    public Mono<User> getUserFromToken(String authorizationHeader) {
        return extractUserIdFromToken(authorizationHeader)
                .flatMap(userRepository::findById)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.")));
    }

    /** Beaer 토큰에서 실제 토큰 값 추출 */
    private String parseToken(String authorizationHeader) {
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ReportableError(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다.", 401);
        }
        return authorizationHeader.substring(7);
    }

    /** refreshToken을 사용해 새로운 accessToken 발급 */
    public Mono<String> reIssueAccessToken(String refreshToken) {
        return jwtProvider.getUserIdFromToken(refreshToken)
                .flatMap(userId -> tokenService.getRefreshTokenByUserId(userId)
                        .flatMap(storedToken -> {
                            if (!storedToken.equals(refreshToken)) {
                                return Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "유효하지 않은 refreshToken입니다."));
                            }
                            return Mono.just(jwtProvider.generateAccessToken(userId, new Date()));
                        })
                )
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "유효하지 않은 refreshToken입니다.")));
    }




    public Mono<Map<String, Object>> logout(String accessToken) {
        tokenService.invalidateAccessToken(accessToken);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "로그아웃 성공");
        response.put("data", null);

        return Mono.just(response);
    }



}
