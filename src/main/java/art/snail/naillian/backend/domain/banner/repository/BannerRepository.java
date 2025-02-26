package art.snail.naillian.backend.domain.banner.repository;

import art.snail.naillian.backend.domain.banner.entity.Banner;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BannerRepository extends ReactiveCrudRepository<Banner, Long> {
}
