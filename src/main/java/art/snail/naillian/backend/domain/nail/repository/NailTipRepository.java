package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailTip;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface NailTipRepository extends ReactiveCrudRepository<NailTip, Integer> {

    Flux<NailTip> findAllBy(Pageable page);

}
