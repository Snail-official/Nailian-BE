package art.snail.naillian.backend.domain.auth.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final StringRedisTemplate redisTemplate;

    /** Redis에 accessToken -> refresh Token 저장 및 userId 저장 */
    public void storeTokenPair(String accessToken, String refreshToken, Integer userId) {
        redisTemplate.opsForValue().set("token:" + accessToken, refreshToken, 30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set("refresh:" + refreshToken, userId.toString(), 7, TimeUnit.DAYS);
    }

    /** 유저ID에서 refreshToken 가져오기 */
    public Mono<Integer> getUserIdFromRefreshToken(String refreshToken) {
        return Mono.justOrEmpty(redisTemplate.opsForValue().get("refresh:" + refreshToken))
                .map(Integer::parseInt)
                .switchIfEmpty(Mono.empty());
    }


    /** RefreshToken 검증(AccessToken과 매칭) */
    public Mono<Boolean> validateRefreshToken(String refreshToken) {
        return Mono.justOrEmpty(redisTemplate.opsForValue().get("refresh:" + refreshToken))
                .map(value -> true)
                .defaultIfEmpty(false);
    }

    /** RefreshToken 사용 시 기존 AccessToken 폐기 */
    public void invalidateAccessToken(String accessToken) {
        redisTemplate.delete("token:" + accessToken);
    }

    /** RefreshToken 사용시 기존 RefreshToken도 폐기 */
    public void invalidateRefreshToken(String refreshToken) {
        redisTemplate.delete("refresh:" + refreshToken);
    }
}
