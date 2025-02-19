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

/** 회원가입 + JWT 토큰 발급 담당 */

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final TokenService tokenService;


    /** Access Token 검증 및 사용자 ID 추출 */
    public Mono<Integer> extractUserIdFromToken(String authorizationHeader) {
        return Mono.justOrEmpty(authorizationHeader)
                .map(this::parseToken)
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
        return jwtProvider.getUserIdFromToken(refreshToken) // JWT 서명 검증 및 userId 추출
                .flatMap(userId -> tokenService.getRefreshTokenByUserId(userId) // Redis에서 저장된 refreshToken 가져오기
                        .flatMap(storedToken -> {
                            if (!storedToken.equals(refreshToken)) { // Redis에 저장된 토큰과 비교
                                return Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "유효하지 않은 refreshToken입니다."));
                            }
                            return Mono.just(jwtProvider.generateAccessToken(userId, new Date())); // 새 accessToken 발급
                        })
                )
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "유효하지 않은 refreshToken입니다."))); // 저장된 refreshToken이 없을 경우
    }


    /** 로그아웃 */
    public Mono<Void> logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        }
        return extractUserIdFromToken(accessToken)
                .flatMap(userId -> tokenService.getRefreshTokenByUserId(userId) // userId 기반 refreshToken 조회
                        .flatMap(refreshToken -> {
                            tokenService.invalidateAccessToken(accessToken);
                            tokenService.invalidateRefreshToken(refreshToken); // refreshToken 삭제
                            return Mono.empty();
                        })
                )
                .then();
    }
}
