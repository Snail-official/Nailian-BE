package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailFolderSet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface NailFolderSetRepository extends ReactiveCrudRepository<NailFolderSet, Integer> {
    Flux<NailFolderSet> findAllByFolderId(Integer folderId, Pageable pageable);

    @Query("""
            SELECT nfs.*
            FROM nail_folder_set nfs
            WHERE nfs.id = :folderId
            ORDER BY RAND(:seed)
            LIMIT :limit
            """)
    Flux<NailFolderSet> findAllShuffledByFolderId(Integer folderId, int limit, int seed);

    Mono<Long> countByFolderId(Integer folderId);
}
