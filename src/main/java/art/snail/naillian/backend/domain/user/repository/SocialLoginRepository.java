package art.snail.naillian.backend.domain.user.repository;

import art.snail.naillian.backend.domain.user.entity.SocialLogin;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;


public interface SocialLoginRepository extends ReactiveCrudRepository<SocialLogin, Integer> {
    Mono<SocialLogin> findByPlatformUserId(String platformUserId);
}