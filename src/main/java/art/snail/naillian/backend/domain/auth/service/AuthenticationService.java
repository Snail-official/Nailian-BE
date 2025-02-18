package art.snail.naillian.backend.domain.auth.service;

import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

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
}
