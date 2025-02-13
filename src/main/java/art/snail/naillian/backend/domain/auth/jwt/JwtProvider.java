package art.snail.naillian.backend.domain.auth.jwt;

import art.snail.naillian.backend.errors.ReportableError;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.micrometer.common.lang.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JwtProvider {

    private final SecretKey key;
    private static final long ACCESS_EXPIRATION = 1000 * 60 * 30;
    private static final long REFRESH_EXPIRATION = 1000 * 60 * 30;

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** AccessToken 생성 */
    public String generateAccessToken(Integer userId) {
        return generateAccessToken(userId, new Date());
    }

    public String generateAccessToken(Integer userId, @Nullable Date issueDate) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(issueDate != null ? issueDate : new Date())
                .setExpiration(new Date(issueDate != null ? issueDate.getTime() + ACCESS_EXPIRATION : System.currentTimeMillis() + ACCESS_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** RefreshToken 생성 */
    public String generateRefreshToken(Integer userId) {
        return generateRefreshToken(userId, new Date());
    }

    public String generateRefreshToken(Integer userId, @Nullable Date issueDate) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(issueDate != null ? issueDate : new Date())
                .setExpiration(new Date(issueDate != null ? issueDate.getTime() + REFRESH_EXPIRATION : System.currentTimeMillis() + REFRESH_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 토큰 검증 및 사용자 ID 추출 */
    public Mono<Integer> getUserIdFromToken(String token) {
        try {
            Integer userId = Integer.parseInt(Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject());
            return Mono.just(userId);
        } catch (Exception e) {
            return Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.", 401));
        }
    }

}
