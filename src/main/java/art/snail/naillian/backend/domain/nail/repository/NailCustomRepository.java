package art.snail.naillian.backend.domain.nail.repository;

import art.snail.naillian.backend.domain.nail.entity.NailGroup;
import reactor.core.publisher.Mono;

public interface NailCustomRepository {
    /**
     * 네일 그룹을 생성하거나 이미 존재하는 경우 가져옴
     *
     * @param groupTemplate id 필드를 제외한 네일 그룹
     * @return 생성되거나 이미 존재하던 네일 그룹
     */
    Mono<NailGroup> saveOrGetNailGroup(NailGroup groupTemplate);

    /**
     * 사용자가 네일 팁의 그룹을 이미 생성했는지 확인
     *
     * @return 이미 네일 셋을 생성한 경우 참, 없으면 거짓
     */
    Mono<Boolean> checkUserHasNailSetByTemplate(int userId, NailGroup template);
}
