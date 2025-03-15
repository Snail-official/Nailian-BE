package art.snail.naillian.backend.domain.nail.service;

import art.snail.naillian.backend.domain.nail.dto.NailSetEmbedDTO;
import art.snail.naillian.backend.domain.nail.entity.NailAssets;
import art.snail.naillian.backend.domain.nail.entity.NailGroup;
import art.snail.naillian.backend.domain.nail.entity.NailSet;
import art.snail.naillian.backend.domain.nail.entity.NailTip;
import art.snail.naillian.backend.domain.nail.repository.NailAssetRepository;
import art.snail.naillian.backend.domain.nail.repository.NailGroupRepository;
import art.snail.naillian.backend.domain.nail.repository.NailSetRepository;
import art.snail.naillian.backend.domain.nail.repository.NailTipRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class NailService {
    private final NailAssetRepository assetRepository;
    private final NailTipRepository tipRepository;
    private final NailSetRepository setRepository;
    private final NailGroupRepository groupRepository;

    public Flux<NailAssets> getNailAssets(Pageable page) {
        return assetRepository.findAllBy(page);
    }

    public Flux<NailTip> getNailTips(Pageable page) {
        return tipRepository.findAllBy(page);
    }

    public Flux<NailSet> getNailSets(Pageable page) {
        return setRepository.findAllBy(page);
    }

    public Mono<NailSet> getNailSet(Integer id) {
        return setRepository.findById(id);
    }

    public Mono<NailGroup> getNailGroup(Integer id) {
        return groupRepository.findById(id);
    }

    public Flux<NailTip> findAllById(List<Integer> ids) {
        return tipRepository.findAllById(ids);
    }

    public Mono<NailTip> getNailTipById(int id) {
        return tipRepository.findById(id)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "네일 팁을 찾을 수 없습니다.")));
    }



    public Flux<NailTip> getNailsBySetId(Integer setId) {
        return this.getNailSet(setId)
                .flatMap(nailSet -> groupRepository.findById(nailSet.getNailGroupId()))
                .flatMapMany(nailGroup -> {
                    List<Integer> ids = Stream.of(
                            nailGroup.getFingerThumb(),
                            nailGroup.getFingerIndex(),
                            nailGroup.getFingerMiddle(),
                            nailGroup.getFingerRing(),
                            nailGroup.getFingerPinky()
                    ).filter(Objects::nonNull).toList();

                    if (ids.size() != 5)
                        return Flux.error(new ReportableError(HttpStatus.BAD_GATEWAY, String.format(
                                "네일 그룹이 올바르게 구성되지 않았습니다. (크기: %s)", ids.size()
                        )));
                    return Flux.fromIterable(ids);
                })
                .flatMapSequential(tipRepository::findById);
    }

    public Mono<NailSetEmbedDTO<NailTip>> getNailSetWithNailTip(Integer setId) {
        return getNailsBySetId(setId)
                .collectList()
                .map(nailTips -> new NailSetEmbedDTO<>(setId, nailTips));
    }

    public Flux<NailSet> getUserNailSets(Integer userId, Pageable page) {
        return setRepository.findAllByUploadedBy(userId, page);
    }

    public Mono<NailSet> createUserNailSet(Integer userId, List<Integer> tipIds) {
        if (tipIds == null || tipIds.size() != 5)
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "만들고자 하는 네일 그룹의 네일 아이디 개수는 5개이어야 합니다."));

        return Mono.just(NailGroup.fromList(tipIds))
                .flatMap(groupRepository::save)
                .map(nailGroup -> NailSet.builder()
                        .nailGroupId(nailGroup.getId())
                        .uploadedBy(userId)
                        .name("사용자가 찜한 네일셋")
                        .build())
                .flatMap(setRepository::save);
    }

    public Flux<NailTip> findByCategoryColorShape(List<Integer> categoryIndices, List<Integer> colorIndices, List<Integer> shapeIndices) {
        return tipRepository.findByCategoryColorShape(categoryIndices, colorIndices, shapeIndices);
    }

}
