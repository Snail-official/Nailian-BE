package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailAssets;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface NailAssetRepository extends ReactiveCrudRepository<NailAssets, Integer> {

    Flux<NailAssets> findAllBy(Pageable page);

}
