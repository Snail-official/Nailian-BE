package art.snail.naillian.backend.domain.user.repository;

import art.snail.naillian.backend.domain.user.entity.UserPersonalNail;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserPersonalNailRepository extends ReactiveCrudRepository<UserPersonalNail, Integer> {
    Mono<UserPersonalNail> findByUserId(Integer userId);
}
