package art.snail.naillian.backend.domain.nail.service;

import art.snail.naillian.backend.domain.nail.dto.NailSetRecommendationDTO;
import art.snail.naillian.backend.domain.nail.entity.*;
import art.snail.naillian.backend.domain.nail.repository.NailGroupRepository;
import art.snail.naillian.backend.domain.nail.repository.NailSetRepository;
import art.snail.naillian.backend.domain.nail.repository.NailTipRepository;
import art.snail.naillian.backend.domain.nail.util.NailAttributeSampler;
import art.snail.naillian.backend.domain.user.entity.UserPreferences;
import art.snail.naillian.backend.domain.user.repository.UserPreferenceRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NailSetRecommendationService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final NailSetRepository nailSetRepository;
    private final NailTipRepository nailTipRepository;
    private final NailGroupRepository nailGroupRepository;

    /**
     * 사용자 선호 기반 추천 네일 세트 조회 (비동기, 최소 메모리 사용)
     *
     * @param userId 사용자 ID
     * @param numSamples 추천 조합 수
     * @param temperature softmax 온도 파라미터
     * @return 추천 네일 세트 DTO 리스트
     */
    public Mono<List<NailSetRecommendationDTO>> getRecommendedNailSets(int userId, int numSamples, double temperature) {
        return userPreferenceRepository.findAllByUserId(userId)
                .collectList()
                .flatMap(preferences -> {
                    if (preferences.isEmpty()) {
                        return Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자 선호 데이터가 없습니다."));
                    }
                    double[][] weightMatrix = buildWeightMatrix(preferences);
                    int[][] combos = NailAttributeSampler.sampleNailAttributesFromMatrix(weightMatrix, numSamples, temperature);
                    return nailSetRepository.findAll()
                            .collectList()
                            .flatMap(allSets -> {
                                if (allSets.isEmpty()) {
                                    return Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "NailSet 데이터가 없습니다."));
                                }
                                return Flux.fromIterable(allSets)
                                        .flatMap(nailSet -> matchesAnyCombo(nailSet, combos)
                                                .filter(match -> match)
                                                .map(match -> nailSet)
                                        )
                                        .collectList()
                                        .flatMap(filteredSets -> {
                                            if (filteredSets.isEmpty()) {
                                                return Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "추천 네일 세트를 찾을 수 없습니다."));
                                            }
                                            return groupByStyleReactive(filteredSets);
                                        });
                            });
                });
    }

    private double[][] buildWeightMatrix(List<UserPreferences> preferences) {
        int numColors = 8;
        int numShapes = 5;
        int numCategories = 4;
        double[][] matrix = new double[3][];
        matrix[0] = new double[numColors];
        matrix[1] = new double[numShapes];
        matrix[2] = new double[numCategories];
        for (UserPreferences up : preferences) {
            matrix[0][(int) up.getColor()] += 1;
            matrix[1][(int) up.getShape()] += 1;
            matrix[2][(int) up.getCategory()] += 1;
        }
        return matrix;
    }

    /**
     * NailSet의 NailGroup(손가락별 NailTip)을 조회하여,
     * 각 손가락의 NailTip들이 추천 조합(색상, 쉐입, 패턴) 중 하나와 모두 매칭되는지 확인
     */
    private Mono<Boolean> matchesAnyCombo(NailSet nailSet, int[][] combos) {
        return nailGroupRepository.findById(nailSet.getNailGroupId())
                .flatMap(nailGroup -> {
                    List<Integer> tipIds = List.of(
                            nailGroup.getFingerThumb(),
                            nailGroup.getFingerIndex(),
                            nailGroup.getFingerMiddle(),
                            nailGroup.getFingerRing(),
                            nailGroup.getFingerPinky()
                    );
                    return Flux.fromIterable(tipIds)
                            .flatMap(tipId -> nailTipRepository.findById(tipId)
                                    .defaultIfEmpty(new NailTip(
                                            0,
                                            // entity 패키지의 enum 사용
                                            NailShape.SQUARE,
                                            NailColor.WHITE,
                                            NailCategory.ONE_COLOR,
                                            "https://example.com/default.jpg",
                                            null, null, 0
                                    )))
                            .collectList()
                            .map(tips -> {
                                if (tips.stream().anyMatch(t -> t.getId() == 0)) {
                                    return false;
                                }
                                for (NailTip tip : tips) {
                                    int colorIdx = tip.getColor().getIndex();
                                    int shapeIdx = tip.getShape().getIndex();
                                    int categoryIdx = tip.getCategory().getIndex();
                                    boolean matchFound = false;
                                    for (int[] combo : combos) {
                                        if (combo[0] == colorIdx && combo[1] == shapeIdx && combo[2] == categoryIdx) {
                                            matchFound = true;
                                            break;
                                        }
                                    }
                                    if (!matchFound) {
                                        return false;
                                    }
                                }
                                return true;
                            });
                })
                .defaultIfEmpty(false);
    }

    /**
     * 추천 NailSet 리스트를 각 NailSet의 NailGroup 정보를 조회하여 DTO로 변환한 후,
     * 스타일별로 그룹화하는 비동기 로직.
     */
    private Mono<List<NailSetRecommendationDTO>> groupByStyleReactive(List<NailSet> sets) {
        return Flux.fromIterable(sets)
                .flatMap(nailSet ->
                        nailGroupRepository.findById(nailSet.getNailGroupId())
                                .flatMap(nailGroup ->
                                        Mono.zip(
                                                nailTipRepository.findById(nailGroup.getFingerThumb())
                                                        .map(NailTip::getImageUrl)
                                                        .defaultIfEmpty("https://example.com/default_thumb.jpg"),
                                                nailTipRepository.findById(nailGroup.getFingerIndex())
                                                        .map(NailTip::getImageUrl)
                                                        .defaultIfEmpty("https://example.com/default_index.jpg"),
                                                nailTipRepository.findById(nailGroup.getFingerMiddle())
                                                        .map(NailTip::getImageUrl)
                                                        .defaultIfEmpty("https://example.com/default_middle.jpg"),
                                                nailTipRepository.findById(nailGroup.getFingerRing())
                                                        .map(NailTip::getImageUrl)
                                                        .defaultIfEmpty("https://example.com/default_ring.jpg"),
                                                nailTipRepository.findById(nailGroup.getFingerPinky())
                                                        .map(NailTip::getImageUrl)
                                                        .defaultIfEmpty("https://example.com/default_pinky.jpg")
                                        ).map(tuple -> new NailSetRecommendationDTO.NailSetDTO(
                                                nailSet.getId(),
                                                new NailSetRecommendationDTO.NailImageDTO(tuple.getT1()),
                                                new NailSetRecommendationDTO.NailImageDTO(tuple.getT2()),
                                                new NailSetRecommendationDTO.NailImageDTO(tuple.getT3()),
                                                new NailSetRecommendationDTO.NailImageDTO(tuple.getT4()),
                                                new NailSetRecommendationDTO.NailImageDTO(tuple.getT5())
                                        ))
                                )
                )
                .collectList()
                .map(list -> {
                    Map<Integer, List<NailSetRecommendationDTO.NailSetDTO>> grouped =
                            list.stream().collect(Collectors.groupingBy(dto -> (dto.getId() % 2 == 0) ? 3 : 1));
                    List<NailSetRecommendationDTO> result = new ArrayList<>();
                    grouped.forEach((styleId, dtos) -> {
                        String styleName = (styleId == 1) ? "TREND" : (styleId == 3) ? "MODERN" : "UNKNOWN";
                        result.add(new NailSetRecommendationDTO(new NailSetRecommendationDTO.StyleDTO(styleId, styleName), dtos));
                    });
                    return result;
                });
    }
}
