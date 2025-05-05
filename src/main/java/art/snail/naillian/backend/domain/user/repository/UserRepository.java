package art.snail.naillian.backend.domain.user.repository;

import art.snail.naillian.backend.domain.user.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Integer> {

    @Query("""
    SELECT *
    FROM user
    WHERE nickname = :nickname
      AND deleted_at IS NULL
""")
    Mono<User> findByNickname(String nickname);
}
