package art.snail.naillian.backend.domain.user.repository;

import art.snail.naillian.backend.domain.user.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Integer> {
    Mono<User> findByNickname(String nickname);
}
