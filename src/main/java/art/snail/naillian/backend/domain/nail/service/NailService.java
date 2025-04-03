package art.snail.naillian.backend.domain.nail.service;

import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.nail.dto.*;
import art.snail.naillian.backend.domain.nail.entity.*;
import art.snail.naillian.backend.domain.nail.repository.*;
import art.snail.naillian.backend.domain.user.entity.UserPreferences;
import art.snail.naillian.backend.domain.user.repository.UserPreferenceRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class NailService {
    private static final Logger log = LoggerFactory.getLogger(NailService.class);
    private final NailAssetRepository assetRepository;
    private final NailTipRepository tipRepository;
    private final NailSetRepository setRepository;
    private final NailGroupRepository groupRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final NailFolderSetRepository folderSetRepository;
    private final NailFolderRepository folderRepository;
    private final NailCustomRepository customRepository;

    /**
     * 사용자의 취향으로 저장된 {@link NailTip} 을 가져옴
     * @return {@link PageDTO} of {@link NailTip}
     */
    public Mono<PageDTO<NailTip>> getUserNailPreferences(int userId, Pageable pageable) {
        return userPreferenceRepository.findAllByUserId(userId, pageable)
                .map(UserPreferences::getTipId)
                .collectList()
                .flatMap(tipIds -> tipRepository.findAllById(tipIds).collectList())
                .zipWith(userPreferenceRepository.countByUserId(userId))
                .map((tuple) -> new PageDTO<NailTip>(tuple.getT1(), pageable, tuple.getT2()))
                ;
    }

    /**
     * 사용자가 선택한 네일 스타일을 저장함
     * 조건 : 최소 3개 이상 네일 스타일 및 최대 10개까지
     * 요청받은 각 네일 스타일 id를 기준으로 네일팁 조회
     */

    public Mono<Void> saveNailPreferences(int userId, SaveNailPreferencesDTO dto) {
        List<Integer> preferences = dto.getPreferences();

        if (preferences.size() < 3 || preferences.size() > 10) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST,
                    "네일 스타일은 최소 3개, 최대 10개까지 선택해야 합니다."));
        }

        return tipRepository.findAllById(preferences)
                .collectList()
                .flatMap(tips -> {
                    if (tips.size() != preferences.size()) {
                        return Mono.error(new ReportableError(HttpStatus.NOT_FOUND,
                                "일부 네일 스타일을 찾을 수 없습니다."));
                    }
                    List<UserPreferences> newPreferences = tips.stream()
                            .map(tip -> new UserPreferences(userId, tip))
                            .toList();

                    return userPreferenceRepository.deleteAllByUserId(userId)
                            .then(userPreferenceRepository.saveAll(newPreferences).then());
                });
    }

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

    public Mono<Long> getUserNailSetCount(Integer userId) {
        return setRepository.countByUploadedBy(userId);
    }

    public Flux<NailSet> getUserNailSets(Integer userId, Pageable page) {
        return setRepository.findAllByUploadedBy(userId, page);
    }

    public Mono<NailSet> createUserNailSet(Integer userId, List<Integer> tipIds) {
        if (tipIds == null || tipIds.size() != 5)
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "만들고자 하는 네일 그룹의 네일 아이디 개수는 5개이어야 합니다."));

        return Mono.just(NailGroup.fromList(tipIds))
                .flatMap(template -> customRepository.checkUserHasNailSetByTemplate(userId, template)
                        .filter(hasSet -> !hasSet)
                        .flatMap(flag -> customRepository.saveOrGetNailGroup(template))
                        .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.CONFLICT, "이미 존재하는 네일 세트입니다.")))
                )
                .map(nailGroup -> NailSet.builder()
                        .nailGroupId(nailGroup.getId())
                        .uploadedBy(userId)
                        .build())
                .flatMap(setRepository::save);
    }

    /**
     * 사용자가 네일 세트를 보관함에 저장함
     *
     * @param userId 사용자의 id, 유효성을 검증하지 않음
     * @param setId  네일 세트의 id, 유효성을 검증함
     * @return 새로 생성되거나 이미 보관했던 네일 세트 정보
     */
    @Transactional
    public Mono<NailSet> cloneNailSetForUser(int userId, int setId) {
        return getNailSet(setId)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "해당 네일 세트를 찾을 수 없습니다.")))
                .flatMap(originalSet -> setRepository.existsByUploadedByAndNailGroupId(userId, originalSet.getNailGroupId())
                        .filter(exists -> !exists)
                        .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.CONFLICT, "이미 저장된 네일 세트입니다.")))
                        .then(Mono.just(originalSet))
                )
                .map(originalSet -> NailSet.builder()
                        .nailGroupId(originalSet.getNailGroupId())
                        .uploadedBy(userId)
                        .build())
                .flatMap(setRepository::save)
                ;
    }


    public Mono<NailSet> deleteNailSetEnsureUser(int userId, int setId) {
        return getNailSet(setId)
                .filter(nailSet -> nailSet.getUploadedBy() == userId)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "해당 네일 세트를 찾을 수 없습니다.")))
                .flatMap(nailSet -> setRepository.delete(nailSet).then(Mono.just(nailSet)));
    }

    /**
     * 현재는 스타일 파라미터를 지정시 랜덤 Shuffle이 들어갑니다.
     */
    public Mono<PageDTO<NailSetEmbedDTO<NailImageUrlDTO>>> getNailSetSimilar(Long nailSetId, int folderId, Pageable pageable) {
        if (folderId <= 0) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "스타일을 지정해야 합니다."));
        }
        return folderSetRepository.findAllByFolderId(folderId, pageable)

                .filter(nfs -> !nfs.getSetId().equals(nailSetId.intValue()))
                .flatMap(nfs ->
                        getNailSetWithNailTip(nfs.getSetId())
                                .map(nailSetEmbed -> nailSetEmbed.transform(NailImageUrlDTO::from))
                )
                .collectList()
                .zipWith(folderSetRepository.countByFolderId(folderId))
                .map(tuple -> {
                    List<NailSetEmbedDTO<NailImageUrlDTO>> list = tuple.getT1();

                    if (list.isEmpty()) {
                        throw new ReportableError(HttpStatus.NOT_FOUND, "유사한 네일 세트를 찾을 수 없습니다.");
                    }

                    Collections.shuffle(list);
                    return new PageDTO<>(list, pageable, tuple.getT2());
                });
    }

    public Mono<Page<NailIdAndUrlDTO>> getNailTipsByAttributes(String shape, String color, String category, Pageable page) {
        String searchingShape = (shape != null) ? shape : "";
        String searchingColor = (color != null) ? color : "";
        String searchingCategory = (category != null) ? category : "";

        return tipRepository.findAllFilteredByShapeAndColorAndCategory(searchingShape, searchingColor, searchingCategory, page.getPageSize(), page.getOffset())
                .map(NailIdAndUrlDTO::from)
                .collectList()
                .zipWith(tipRepository.countFilteredByShapeAndColorAndCategory(searchingShape, searchingColor, searchingCategory))
                .handle((tuple, sink) -> sink.next(new PageDTO<>(tuple.getT1(), page, tuple.getT2())))
                ;
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

    public Mono<List<NailSetRecommendationDTO>> getRecommendedNailSets(int limit) {
        // 3개의 폴더 ID만 사용: 1, 2, 3
        List<Integer> folderIds = List.of(1, 2, 3);

        Flux<NailFolder> folderChain = Flux.fromIterable(folderIds)
                .flatMapSequential(folderRepository::findById);
        Flux<List<NailSetEmbedDTO<NailImageUrlDTO>>> entriesChain = Flux.fromIterable(folderIds)
                .flatMapSequential(folderId -> folderSetRepository.findAllShuffledByFolderId(folderId, limit, ThreadLocalRandom.current().nextInt())
                        .flatMap(nfs -> getNailSetWithNailTip(nfs.getSetId()))
                        .map(dto -> dto.transform(NailImageUrlDTO::from))
                        .collectList()
                );

        return folderChain.zipWith(entriesChain)
                .<NailSetRecommendationDTO>handle((tuple, sink) -> {
                    sink.next(new NailSetRecommendationDTO(
                            tuple.getT1().getId().longValue(),
                            tuple.getT1().getName(),
                            tuple.getT2()
                    ));
                })
                .collectList();
    }


}
