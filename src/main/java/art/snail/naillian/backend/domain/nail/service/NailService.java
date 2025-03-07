package art.snail.naillian.backend.domain.nail.service;

import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.nail.dto.NailSetEmbedDTO;
import art.snail.naillian.backend.domain.nail.dto.NailSetRecommendationDTO;
import art.snail.naillian.backend.domain.nail.dto.SaveNailPreferencesDTO;
import art.snail.naillian.backend.domain.nail.entity.*;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class NailService {
    private final NailAssetRepository assetRepository;
    private final NailTipRepository tipRepository;
    private final NailSetRepository setRepository;
    private final NailGroupRepository groupRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final NailSetRecommendationService nailSetRecommendationService;

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
                        .name("사용자가 찜한 네일셋")
                        .build())
                .flatMap(setRepository::save);
    }

    /**
     * 사용자가 선택한 네일 스타일(네일 팁 id 배열)을 저장.
     */
    public Mono<Void> saveNailPreferences(int userId, SaveNailPreferencesDTO dto) {
        List<Integer> preferences = dto.getPreferences();
        if (preferences.size() < 3) {
            return Mono.error(new ReportableError(HttpStatus.UNPROCESSABLE_ENTITY, "최소 3개 이상의 네일을 선택해야 합니다."));
        }
        if (preferences.size() > 10) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "최대 10개까지 선택할 수 있습니다."));
        }
        return tipRepository.findAllById(preferences)
                .collectList()
                .flatMap(tips -> {
                    if (tips.size() != preferences.size()) {
                        return Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "일부 네일 스타일을 찾을 수 없습니다."));
                    }
                    List<UserPreferences> newPreferences = tips.stream()
                            .map(tip -> new UserPreferences(
                                    null,
                                    userId,
                                    tip.getShape() != null ? tip.getShape().getIndex() : 0,
                                    tip.getColor() != null ? tip.getColor().getIndex() : 0,
                                    tip.getCategory() != null ? tip.getCategory().getIndex() : 0
                            ))
                            .collect(Collectors.toList());
                    return userPreferenceRepository.deleteAllByUserId(userId)
                            .then(userPreferenceRepository.saveAll(newPreferences).then());
                });
    }

    /**
     * 추천 네일 세트 조회
     */
    public Mono<List<NailSetRecommendationDTO>> getNailSetRecommendations(int userId) {
        int numSamples = 15;
        double temperature = 1.0;
        return nailSetRecommendationService.getRecommendedNailSets(userId, numSamples, temperature);
    }
}
