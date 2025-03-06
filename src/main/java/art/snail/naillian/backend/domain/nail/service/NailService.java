package art.snail.naillian.backend.domain.nail.service;

import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.nail.common.NailCategory;
import art.snail.naillian.backend.domain.nail.common.NailColor;
import art.snail.naillian.backend.domain.nail.common.NailShape;
import art.snail.naillian.backend.domain.nail.dto.NailSetEmbedDTO;
import art.snail.naillian.backend.domain.nail.dto.SaveNailPreferencesDTO;
import art.snail.naillian.backend.domain.nail.entity.NailAssets;
import art.snail.naillian.backend.domain.nail.entity.NailGroup;
import art.snail.naillian.backend.domain.nail.entity.NailSet;
import art.snail.naillian.backend.domain.nail.entity.NailTip;
import art.snail.naillian.backend.domain.nail.repository.NailAssetRepository;
import art.snail.naillian.backend.domain.nail.repository.NailGroupRepository;
import art.snail.naillian.backend.domain.nail.repository.NailSetRepository;
import art.snail.naillian.backend.domain.nail.repository.NailTipRepository;
import art.snail.naillian.backend.domain.user.entity.UserPreferences;
import art.snail.naillian.backend.domain.user.repository.UserPreferenceRepository;
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
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * UserPreferences 엔티티에는 NailTip id가 저장되지 않고, 네일 스타일의 속성인 shape, color, category가 저장 돼있으므로
     * 해당 속성들을 이용해 NailTip을 조회해야 함
     */
    public Mono<PageDTO<NailTip>> getUserNailPreferences(int userId, Pageable pageable) {
        return userPreferenceRepository.countByUserId(userId)
                .flatMap(totalElements ->
                        userPreferenceRepository.findAllByUserId(userId, pageable)
                                .flatMap(this::convertUserPrefToNailTip)
                                .collectList()
                                .map(list -> new PageDTO<>(list, pageable, totalElements))
                );
    }

    private Mono<NailTip> convertUserPrefToNailTip(UserPreferences up) {
        return tipRepository.findByShapeAndColorAndCategory(
                NailShape.values()[(int) up.getShape()].name().toLowerCase(),
                NailColor.values()[(int) up.getColor()].name().toLowerCase(),
                NailCategory.values()[(int) up.getCategory()].name().toLowerCase()
        );
    }

    /**
     * 사용자가 선택한 네일 스타일을 저장함
     * 조건 : 최소 3개 이상 네일 스타일 및 최대 10개까지
     * 요청받은 각 네일 스타일 id를 기준으로 네일팁 조회
     * 네일팁 속성(enum의 ordinal 값을 double로 변환) 이용해 UserPreferences 엔티티 생성 후 저장
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
                            .map(tip -> new UserPreferences(null, userId,
                                    tip.getShape(),
                                    tip.getColor(),
                                    tip.getCategory()))
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
    public Mono<NailSet> cloneNailSetForUser(int userId, int setId) {
        return getNailSet(setId)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "해당 네일 세트를 찾을 수 없습니다.")))
                .flatMap(originalSet ->
                        // 이미 저장한 적 있는 경우 이미 저장된 정보를 제공함
                        setRepository.findByUploadedByAndNailGroupId(userId, originalSet.getNailGroupId())
                                // 저장한 적 없는 경우 새로 저장함
                                .switchIfEmpty(setRepository.save(NailSet.builder()
                                        .nailGroupId(originalSet.getNailGroupId())
                                        .uploadedBy(userId)
                                        .build()))
                );
    }

    public Mono<NailSet> deleteNailSetEnsureUser(int userId, int setId) {
        return getNailSet(setId)
                .filter(nailSet -> nailSet.getUploadedBy() == userId)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "해당 네일 세트를 찾을 수 없습니다.")))
                .flatMap(nailSet -> setRepository.delete(nailSet).then(Mono.just(nailSet)));
    }
}
