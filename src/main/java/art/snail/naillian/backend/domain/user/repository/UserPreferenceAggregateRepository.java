package art.snail.naillian.backend.domain.user.repository;

import art.snail.naillian.backend.domain.user.entity.UserPreferenceAggregate;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserPreferenceAggregateRepository extends ReactiveCrudRepository<UserPreferenceAggregate, Integer> {
    Mono<Void> deleteByUserId(int userId);
    Mono<UserPreferenceAggregate> findByUserId(int userId);
}
