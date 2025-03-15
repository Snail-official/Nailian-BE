package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailTip;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.List;

public interface NailTipRepository extends ReactiveCrudRepository<NailTip, Integer> {

    Flux<NailTip> findAllBy(Pageable page);

    @Query("""
    SELECT * FROM nail_tip
    WHERE category IN (:categoryIndices)
    AND color IN (:colorIndices)
    AND shape IN (:shapeIndices)
""")
    Flux<NailTip> findByCategoryColorShape(List<Integer> categoryIndices, List<Integer> colorIndices, List<Integer> shapeIndices);

}
