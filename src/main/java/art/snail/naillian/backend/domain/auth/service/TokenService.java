package art.snail.naillian.backend.domain.auth.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final StringRedisTemplate redisTemplate;

    /** Redis에 accessToken -> refresh Token 저장 및 userId 저장 */
    public void storeTokenPair(String accessToken, String refreshToken, Integer userId){
        redisTemplate.opsForValue().set("token" + accessToken, refreshToken, 30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set("refresh" + refreshToken, userId.toString(), 7, TimeUnit.DAYS);
    }

    /** RefreshToken 검증(AccessToken과 매칭) */
    public boolean validateRefreshToken(String accessToken, String refreshToken){
        String storedRefreshToken = redisTemplate.opsForValue().get("token" + accessToken);
        return storedRefreshToken != null && storedRefreshToken.equals(refreshToken);
    }

    /** RefreshToken 사용 시 기존 AccessToken 폐기 */
    public void invalidateAccessToken(String accessToken){
        redisTemplate.delete("token" + accessToken);
    }

    /** RefreshToken 사용시 기존 RefreshToken도 폐기 */
    public void invalidateRefreshToken(String refreshToken){
        redisTemplate.delete("refresh" + refreshToken);
    }
}
