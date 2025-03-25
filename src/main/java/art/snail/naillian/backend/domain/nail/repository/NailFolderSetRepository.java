package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailFolderSet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface NailFolderSetRepository extends ReactiveCrudRepository<NailFolderSet, Integer> {
    Flux<NailFolderSet> findAllByFolderId(Integer folderId, Pageable pageable);

    Mono<Long> countByFolderId(Integer folderId);
}
