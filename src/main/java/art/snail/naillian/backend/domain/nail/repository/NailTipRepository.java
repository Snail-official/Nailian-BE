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
         WHERE deleted_at IS NULL
           AND shape    = :shape
           AND color    = :color
           AND category = :category
         LIMIT 1
    """)
    Mono<NailTip> findByShapeAndColorAndCategory(String shape, String color, String category);

    @Query("""
                SELECT *
                FROM nail_tip
                WHERE deleted_at IS NULL
                  AND ('' = :shape or shape = :shape)
                  AND ('' = :color or color = :color)
                  AND ('' = :category or category = :category)
                LIMIT :limit
                OFFSET :offset
            """)
    Flux<NailTip> findAllFilteredByShapeAndColorAndCategory(
            String shape,
            String color,
            String category,
            int limit,
            long offset
    );

    @Query("""
        SELECT COUNT(*)
          FROM nail_tip
         WHERE deleted_at IS NULL
           AND ('' = :shape    OR shape    = :shape)
           AND ('' = :color    OR color    = :color)
           AND ('' = :category OR category = :category)
    """)
    Mono<Long> countFilteredByShapeAndColorAndCategory(
            String shape,
            String color,
            String category
    );

    @Query("""
            SELECT *
            FROM nail_tip
            WHERE deleted_at IS NULL
            ORDER BY RAND(:seed)
            LIMIT :#{#pageable.getPageSize()}
            OFFSET :#{#pageable.getOffset()}
            """)
    Flux<NailTip> findAllShuffled(Pageable pageable, int seed);
}
