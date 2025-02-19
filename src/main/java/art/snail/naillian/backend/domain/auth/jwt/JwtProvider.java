package art.snail.naillian.backend.domain.auth.jwt;

import art.snail.naillian.backend.errors.ReportableError;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.micrometer.common.lang.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JwtProvider {

    private final SecretKey key;
    private static final long ACCESS_EXPIRATION = 1000 * 60 * 30;
    private static final long REFRESH_EXPIRATION = 1000 * 60 * 60 * 24 * 7;

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** AccessToken 생성 */
    public String generateAccessToken(Integer userId, @Nullable Date issueDate) {
        Date now = issueDate != null ? issueDate : new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)  // 동일한 발행 시점 유지
                .setExpiration(new Date(now.getTime() + ACCESS_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Integer userId, @Nullable Date issueDate) {
        Date now = issueDate != null ? issueDate : new Date();
        String refreshTokenId = UUID.randomUUID().toString();

        return Jwts.builder()
                .setSubject(userId + "-" + refreshTokenId)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 토큰 검증 및 사용자 ID 추출 */
    public Mono<Integer> getUserIdFromToken(String token) {
        return Mono.fromCallable(() ->
                        Integer.parseInt(Jwts.parserBuilder()
                                .setSigningKey(key)
                                .build()
                                .parseClaimsJws(token)
                                .getBody()
                                .getSubject()))
                .onErrorMap(e -> new ReportableError(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
    }
}
