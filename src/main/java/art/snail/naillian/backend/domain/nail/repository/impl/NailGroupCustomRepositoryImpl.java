package art.snail.naillian.backend.domain.nail.repository.impl;

import art.snail.naillian.backend.domain.nail.entity.NailGroup;
import art.snail.naillian.backend.domain.nail.repository.NailGroupCustomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class NailGroupCustomRepositoryImpl implements NailGroupCustomRepository {
    private static final String QUERY_SAVE_OR_GET = """
            BEGIN NOT ATOMIC
                 DECLARE GROUP_ID INT;

                 SELECT ng.id INTO GROUP_ID FROM nail_group ng
                 WHERE finger_thumb = :f1 AND finger_index = :f2 AND finger_middle = :f3 AND finger_ring = :f4 AND finger_pinky = :f5 LIMIT 1;

                 IF GROUP_ID is null
                 THEN
                     INSERT INTO nail_group (finger_thumb, finger_index, finger_middle, finger_ring, finger_pinky) VALUE (:f1, :f2, :f3, :f4, :f5)
                     RETURNING nail_group.*;
                 ELSE
                     SELECT * FROM nail_group WHERE id = GROUP_ID;
                 END IF;
             END;""";
    private final DatabaseClient client;
    private final MappingR2dbcConverter converter;

    /**
     * 네일 그룹을 생성하거나 이미 존재하는 경우 가져옴
     * <p>
     * 복합적인 Procedure 로 네일 그룹이 존재하는지 먼저 조회한 후 그룹 id 가 null 인 경우 새로 생성한 id 를 반환합니다.
     * Critical Section 문제로 인해 Transaction 으로 조회합니다.
     */
    @Transactional
    @Modifying
    @Override
    public Mono<NailGroup> saveOrGet(NailGroup groupTemplate) {
        return client.sql(QUERY_SAVE_OR_GET)
                .bind("f1", groupTemplate.getFingerThumb())
                .bind("f2", groupTemplate.getFingerIndex())
                .bind("f3", groupTemplate.getFingerMiddle())
                .bind("f4", groupTemplate.getFingerRing())
                .bind("f5", groupTemplate.getFingerPinky())
                .map((row, meta) -> converter.read(NailGroup.class, row, meta))
                .one()
                .single()
                ;
    }
}
