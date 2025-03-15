package art.snail.naillian.backend.domain.user.repository;

import art.snail.naillian.backend.domain.user.entity.UserPreferences;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;


@Repository
public interface UserPreferenceRepository extends ReactiveCrudRepository<UserPreferences, Integer> {
    Flux<UserPreferences> findAllByUserId(Integer userId);
}
