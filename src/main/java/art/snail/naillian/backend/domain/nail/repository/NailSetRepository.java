package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailSet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NailSetRepository extends ReactiveCrudRepository<NailSet, Integer> {
    Flux<NailSet> findAllBy(Pageable page);

    Flux<NailSet> findAllByUploadedBy(Integer uploadedUserId, Pageable page);

    Mono<NailSet> findByUploadedByAndNailGroupId(int uploadedUserId, int groupId);
}
