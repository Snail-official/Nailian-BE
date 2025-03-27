package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailGroup;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;

public interface NailGroupCustomRepository {
    /**
     * 네일 그룹을 생성하거나 이미 존재하는 경우 가져옴
     *
     * @param groupTemplate id 필드를 제외한 네일 그룹
     * @return 생성되거나 이미 존재하던 네일 그룹
     */
    Mono<NailGroup> saveOrGet(@Param("ng") NailGroup groupTemplate);
}
