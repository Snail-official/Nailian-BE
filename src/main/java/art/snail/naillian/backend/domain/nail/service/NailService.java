package art.snail.naillian.backend.domain.nail.service;

import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.nail.dto.NailImageUrlDTO;
import art.snail.naillian.backend.domain.nail.dto.NailSetEmbedDTO;
import art.snail.naillian.backend.domain.nail.entity.NailAssets;
import art.snail.naillian.backend.domain.nail.entity.NailGroup;
import art.snail.naillian.backend.domain.nail.entity.NailSet;
import art.snail.naillian.backend.domain.nail.entity.NailTip;
import art.snail.naillian.backend.domain.nail.repository.NailAssetRepository;
import art.snail.naillian.backend.domain.nail.repository.NailGroupRepository;
import art.snail.naillian.backend.domain.nail.repository.NailSetRepository;
import art.snail.naillian.backend.domain.nail.repository.NailTipRepository;
import art.snail.naillian.backend.domain.nail.repository.*;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
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
    private final NailFolderSetRepository folderSetRepository;

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
                .flatMap(nailTips -> {
                    if (nailTips.size() != 5) {
                        return Mono.empty(); // 방어 코드 추가
                    }
                    return Mono.just(new NailSetEmbedDTO<>(setId, nailTips));
                });
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

    public Mono<PageDTO<NailSetEmbedDTO<NailImageUrlDTO>>> getNailSetFeed(int folderId, Pageable pageable) {
        if (folderId <= 0) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "스타일을 지정해야 합니다."));
        }
        return folderSetRepository.findAllByFolderId(folderId, pageable)
                .flatMap(nfs ->
                        getNailSetWithNailTip(nfs.getSetId())
                                .map(nailSetEmbed -> nailSetEmbed.transform(NailImageUrlDTO::from))
                )
                .collectList()
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        return Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "해당 스타일의 네일 세트를 찾을 수 없습니다."));
                    }
                    Collections.shuffle(list);
                    return folderSetRepository.countByFolderId(folderId)
                            .map(total -> new PageDTO<>(list, pageable, total));
                });
    }

}
