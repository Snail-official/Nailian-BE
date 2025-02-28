package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailSet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface NailSetRepository extends ReactiveCrudRepository<NailSet, Integer> {
    Flux<NailSet> findAllBy(Pageable page);

}
