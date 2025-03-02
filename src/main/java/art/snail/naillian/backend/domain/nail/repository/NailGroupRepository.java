package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailGroup;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface NailGroupRepository extends ReactiveCrudRepository<NailGroup, Integer> {
    Flux<NailGroup> findAllBy(Pageable page);

}
