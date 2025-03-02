package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailSet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface NailSetRepository extends ReactiveCrudRepository<NailSet, Integer> {
    Flux<NailSet> findAllBy(Pageable page);

    @Query("""
        SELECT ns.*
        FROM nail_folder_set nfs
        JOIN nail_set ns ON ns.id = nfs.set_id
        WHERE nfs.folder_id = :folderId
        ORDER BY ns.id
        LIMIT :#{#page.pageSize}
        OFFSET :#{#page.offset}
    """)
    Flux<NailSet> findByFolderId(@Param("folderId") Integer folderId, Pageable page);
}
