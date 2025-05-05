package art.snail.naillian.backend.domain.user.repository;

import art.snail.naillian.backend.domain.user.entity.SocialLogin;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface SocialLoginRepository extends ReactiveCrudRepository<SocialLogin, Integer> {
    @Query("""
    SELECT *
    FROM social_login
    WHERE platform_user_id = :platformUserId
    AND deleted_at IS NULL
    """)
    Mono<SocialLogin> findByPlatformUserId(String platformUserId);

    @Query("""
    SELECT *
    FROM social_login
    WHERE user_id = :userId
    AND deleted_at IS NULL
    """)
    Flux<SocialLogin> findAllByUserId(int userId);
}