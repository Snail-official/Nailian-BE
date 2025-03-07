package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailTip;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NailTipRepository extends ReactiveCrudRepository<NailTip, Integer> {

    Flux<NailTip> findAllBy(Pageable page);

    @Query("""
        SELECT * 
        FROM nail_tip
        WHERE shape = :shape
          AND color = :color
          AND category = :category
        LIMIT 1
        """)
    Mono<NailTip> findByShapeAndColorAndCategory(String shape, String color, String category);
}
