package art.snail.naillian.backend.domain.nail.service;

import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.nail.common.NailCategory;
import art.snail.naillian.backend.domain.nail.common.NailColor;
import art.snail.naillian.backend.domain.nail.common.NailShape;
import art.snail.naillian.backend.domain.nail.dto.NailIdAndUrlDTO;
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
import org.springframework.data.domain.PageRequest;
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
    private final JwtProvider jwtProvider;
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * UserPreferences 엔티티에는 NailTip id가 저장되지 않고, 네일 스타일의 속성인 shape, color, category가 저장 돼있으므로
     * 해당 속성들을 이용해 NailTip을 조회해야 함
     */
    public Mono<PageDTO<NailIdAndUrlDTO>> getUserNailPreferences(int userId, int page, int size) {
        return userPreferenceRepository.findAllByUserId(userId)
                .collectList()
                .flatMap(preferences -> {
                    int totalElements = preferences.size();
                    Pageable pageable = PageRequest.of(page - 1, size);
                    int offset = (int) pageable.getOffset();
                    if (offset >= totalElements) {
                        return Mono.just(new PageDTO<>(Collections.emptyList(), pageable, totalElements));
                    }
                    int end = Math.min(offset + size, totalElements);
                    List<UserPreferences> pagedPreferences = preferences.subList(offset, end);

                    return Flux.fromIterable(pagedPreferences)
                            .flatMap(up -> {
                                int shapeIdx = (int) up.getShape();
                                int colorIdx = (int) up.getColor();
                                int categoryIdx = (int) up.getCategory();

                                NailShape shapeEnum = NailShape.values()[shapeIdx];
                                NailColor colorEnum = NailColor.values()[colorIdx];
                                NailCategory categoryEnum = NailCategory.values()[categoryIdx];

                                String shapeStr = shapeEnum.name().toLowerCase();
                                String colorStr = colorEnum.name().toLowerCase();
                                String categoryStr = categoryEnum.name().toLowerCase();

                                return tipRepository.findByShapeAndColorAndCategory(shapeStr, colorStr, categoryStr)
                                        .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND,
                                                "NailTip을 찾을 수 없습니다. (shape=" + shapeStr +
                                                        ", color=" + colorStr +
                                                        ", category=" + categoryStr + ")")));
                            })
                            .map(NailIdAndUrlDTO::from)
                            .collectList()
                            .map(dtoList -> new PageDTO<>(dtoList, pageable, totalElements));
                });
    }



    /**
     * 사용자가 선택한 네일 스타일을 저장함
     * 조건 : 최소 3개 이상 네일 스타일 및 최대 10개까지
     * 요청받은 각 네일 스타일 id를 기준으로 네일팁 조회
     * 네일팁 속성(enum의 ordinal 값을 double로 변환) 이용해 UserPreferences 엔티티 생성 후 저장
     */

    public Mono<String> saveNailPreferences(int userId, SaveNailPreferencesDTO dto) {
        List<Integer> preferences = dto.getPreferences();

        if (preferences.size() < 3) {
            return Mono.error(new ReportableError(HttpStatus.UNPROCESSABLE_ENTITY, "최소 3개 이상의 네일 스타일을 선택해주세요."));
        }
        if (preferences.size() > 10) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "최대 10개까지만 네일 스타일 선택이 가능합니다."));
        }

        return userPreferenceRepository.deleteAllByUserId(userId)
                .thenMany(Flux.fromIterable(preferences))
                .flatMap(tipId ->
                        tipRepository.findById(tipId)
                                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND,
                                        "해당 네일 스타일을 찾을 수 없습니다. id: " + tipId)))
                                .map(nailTip -> new UserPreferences(
                                        null, userId,
                                        (double) nailTip.getShape().ordinal(),
                                        (double) nailTip.getColor().ordinal(),
                                        (double) nailTip.getCategory().ordinal()
                                ))
                )
                .collectList()
                .flatMapMany(userPreferenceRepository::saveAll)
                .collectList()
                .then(Mono.just("선호 취향 저장 성공"));
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
}
