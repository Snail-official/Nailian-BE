package art.snail.naillian.backend.domain.user.repository;

import art.snail.naillian.backend.domain.user.entity.UserPreferences;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public interface UserPreferenceRepository extends ReactiveCrudRepository<UserPreferences, Integer> {
    Flux<UserPreferences> findAllByUserId(int userId);

    Mono<Long> countByUserId(int userId);

    Mono<Void> deleteAllByUserId(Integer userId);

}
