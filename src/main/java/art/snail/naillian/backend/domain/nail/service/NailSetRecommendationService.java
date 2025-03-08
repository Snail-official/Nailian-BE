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

    public Mono<List<NailSetRecommendationDTO>> getRecommendedNailSets(int userId, int numSamples, double temperature) {
        return userPreferenceRepository.findAllByUserId(userId)
                .collectList()
                .flatMap(preferences -> {
                    if (preferences.isEmpty()) {
                        return Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자 선호 데이터가 없습니다."));
                    }

                    double[][] weightMatrix = buildWeightMatrix(preferences);
                    System.out.println("📊 Weight Matrix 확인:");
                    for (int i = 0; i < weightMatrix.length; i++) {
                        System.out.println("  - " + Arrays.toString(weightMatrix[i]));
                    }

                    int[][] combos = NailAttributeSampler.sampleNailAttributesFromMatrix(weightMatrix, numSamples, temperature);
                    System.out.println("🎯 추천 조합:");
                    for (int[] combo : combos) {
                        System.out.println("  - Color: " + combo[0] + ", Shape: " + combo[1] + ", Category: " + combo[2]);
                    }

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
                                            NailShape.SQUARE,
                                            NailColor.WHITE,
                                            NailCategory.ONE_COLOR,
                                            "https://example.com/default.jpg",
                                            null, null, 0
                                    )))
                            .collectList()
                            .map(tips -> {
                                if (tips.stream().anyMatch(t -> t.getId() == 0)) {
                                    System.out.println("⚠ NailTip 중 일부가 존재하지 않음: " + tips);
                                    return false;
                                }

                                System.out.println("🔍 [NailSet ID: " + nailSet.getId() + "] NailTips 확인:");
                                for (NailTip tip : tips) {
                                    System.out.printf("  - NailTip ID: %d, Color: %d, Shape: %d, Category: %d\n",
                                            tip.getId(),
                                            tip.getColor().getIndex(),
                                            tip.getShape().getIndex(),
                                            tip.getCategory().getIndex()
                                    );
                                }

                                // **매칭 조건 완화**
                                int matchCount = 0;
                                for (NailTip tip : tips) {
                                    int colorIdx = tip.getColor().getIndex();
                                    int shapeIdx = tip.getShape().getIndex();
                                    int categoryIdx = tip.getCategory().getIndex();

                                    for (int[] combo : combos) {
                                        if (combo[0] == colorIdx || combo[1] == shapeIdx || combo[2] == categoryIdx) {
                                            matchCount++;
                                            break; // 한 NailTip이 한 추천 조합과만 매칭되면 됨
                                        }
                                    }
                                }

                                boolean isMatch = (matchCount >= 3); // 5개 중 최소 3개만 매칭되어도 추천 가능
                                if (isMatch) {
                                    System.out.println("✅ NailSet " + nailSet.getId() + "가 추천 조합과 부분 매칭됨!");
                                } else {
                                    System.out.println("❌ NailSet " + nailSet.getId() + "는 추천 조합과 매칭되지 않음.");
                                }
                                return isMatch;
                            });
                })
                .defaultIfEmpty(false);
    }



    private Mono<List<NailSetRecommendationDTO>> groupByStyleReactive(List<NailSet> sets) {
        return Flux.fromIterable(sets)
                .flatMap(nailSet ->
                        nailGroupRepository.findById(nailSet.getNailGroupId())
                                .flatMap(nailGroup ->
                                        Mono.zip(
                                                nailTipRepository.findById(nailGroup.getFingerThumb()).map(NailTip::getImageUrl).defaultIfEmpty("https://example.com/default_thumb.jpg"),
                                                nailTipRepository.findById(nailGroup.getFingerIndex()).map(NailTip::getImageUrl).defaultIfEmpty("https://example.com/default_index.jpg"),
                                                nailTipRepository.findById(nailGroup.getFingerMiddle()).map(NailTip::getImageUrl).defaultIfEmpty("https://example.com/default_middle.jpg"),
                                                nailTipRepository.findById(nailGroup.getFingerRing()).map(NailTip::getImageUrl).defaultIfEmpty("https://example.com/default_ring.jpg"),
                                                nailTipRepository.findById(nailGroup.getFingerPinky()).map(NailTip::getImageUrl).defaultIfEmpty("https://example.com/default_pinky.jpg")
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
                .map(list -> list.stream().map(dto -> new NailSetRecommendationDTO(
                        new NailSetRecommendationDTO.StyleDTO(dto.getId() % 2 == 0 ? 3 : 1, dto.getId() % 2 == 0 ? "MODERN" : "TREND"),
                        List.of(dto)
                )).collect(Collectors.toList()));
    }
}
