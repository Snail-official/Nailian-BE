package art.snail.naillian.backend.domain.auth.service;

import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;


@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    /**
     * Access Token 검증 및 사용자 ID 추출
     */
    public Mono<Integer> extractUserIdFromToken(String accessToken) {
        return Mono.justOrEmpty(accessToken)
                .flatMap(jwtProvider::getUserIdFromToken)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.")));
    }

    /**
     * refreshToken을 사용해 새로운 accessToken 발급
     */
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

    /**
     * 액세스 토큰을 만료시키고 실패하는 경우 Mono.error 를 방출합니다.
     *
     * @param accessToken 만료시키고자 하는 액세스 토큰
     * @return 성공 여부 (true)
     */
    public Mono<Boolean> logout(@NonNull String accessToken) {
        return Mono.just(accessToken)
                .filter(token -> !token.isBlank())
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다.")))
                .then(Mono.fromCallable(() -> {
                    tokenService.invalidateAccessToken(accessToken);
                    return true;
                }).onErrorMap(Throwable.class, (e) -> {
                    log.warn("토큰을 무효화하는데 실패함", e);
                    return new ReportableError(HttpStatus.INTERNAL_SERVER_ERROR, "로그아웃에 실패했습니다.");
                }));
    }
}
