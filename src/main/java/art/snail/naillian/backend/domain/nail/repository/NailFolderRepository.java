package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailFolder;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface NailFolderRepository extends ReactiveCrudRepository<NailFolder, Integer> {
    Mono<NailFolder> findById(Integer id);
}
