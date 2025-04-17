package art.snail.naillian.backend.domain.user.repository;

import art.snail.naillian.backend.domain.user.entity.EventSubmission;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface EventSubmissionRepository extends ReactiveCrudRepository<EventSubmission, Integer> {

    Mono<Boolean> existsByUserId(Integer userId);
}
