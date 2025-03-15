package art.snail.naillian.backend.domain.nail.service;

import art.snail.naillian.backend.domain.nail.dto.NailSetRecommendationDTO;
import art.snail.naillian.backend.domain.nail.entity.NailSet;
import art.snail.naillian.backend.domain.nail.repository.NailGroupRepository;
import art.snail.naillian.backend.domain.nail.repository.NailSetRepository;
import art.snail.naillian.backend.domain.nail.repository.NailTipRepository;
import art.snail.naillian.backend.domain.user.entity.UserPreferenceAggregate;
import art.snail.naillian.backend.domain.user.repository.UserPreferenceAggregateRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NailRecommendationService {
    private final UserPreferenceAggregateRepository userPreferenceAggregateRepository;
    private final NailSetRepository setRepository;
    private final NailTipRepository tipRepository;
    private final NailService nailService;
    private final NailGroupRepository groupRepository;

    /**
     * 사용자 선호도(aggregate)에 따른 추천, 실제 DB에서 NailSet 이미지(5개 손가락)를 가져옴
     */
    public Mono<List<NailSetRecommendationDTO>> getRecommendedNailSets(int userId, int limit) {
        return userPreferenceAggregateRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자 선호 데이터가 없습니다.")))
                .flatMap(aggregate -> setRepository.findAll().collectList()
                        .flatMap(allSets -> {
                            if (allSets.isEmpty()) {
                                return Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "네일 세트가 없습니다."));
                            }
                            // 점수 계산해서 상위 limit만 추출 (예시)
                            return Flux.fromIterable(allSets)
                                    .flatMap(nailSet -> computeScoreForNailSet(nailSet, aggregate)
                                            .flatMap(score -> convertToNailSetDTO(nailSet)
                                                    .map(dto -> new ScoredNailSetDTO(dto, score))
                                            )
                                    )
                                    .collectList()
                                    .map(scoredList -> {
                                        // 점수가 높은 순으로 정렬
                                        scoredList.sort((a, b) -> Double.compare(b.score, a.score));
                                        // 상위 limit
                                        List<ScoredNailSetDTO> topList = scoredList.stream()
                                                .limit(limit)
                                                .collect(Collectors.toList());

                                        // style=null (어떻게 구현할지 고민 중)
                                        NailSetRecommendationDTO dto = new NailSetRecommendationDTO(
                                                null,
                                                topList.stream().map(s -> s.nailSetDTO).collect(Collectors.toList())
                                        );
                                        return List.of(dto);
                                    });
                        })
                );
    }

    /**
     * NailSet → 점수 계산 (aggregate 점수 합산)
     */
    private Mono<Double> computeScoreForNailSet(NailSet nailSet, UserPreferenceAggregate agg) {
        return groupRepository.findById(nailSet.getNailGroupId())
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "NailGroup을 찾을 수 없습니다.")))
                .flatMapMany(nailGroup -> {
                    List<Integer> tipIds = List.of(
                            nailGroup.getFingerThumb(),
                            nailGroup.getFingerIndex(),
                            nailGroup.getFingerMiddle(),
                            nailGroup.getFingerRing(),
                            nailGroup.getFingerPinky()
                    );
                    return tipRepository.findAllById(tipIds);
                })
                .map(tip -> {
                    double score = 0;
                    // category
                    switch (tip.getCategory()) {
                        case ONE_COLOR -> score += agg.getOneColor();
                        case FRENCH -> score += agg.getFrench();
                        case GRADIENT -> score += agg.getGradient();
                        case ART -> score += agg.getArt();
                    }
                    // color
                    switch (tip.getColor()) {
                        case WHITE -> score += agg.getWhite();
                        case BLACK -> score += agg.getBlack();
                        case BEIGE -> score += agg.getBeige();
                        case PINK -> score += agg.getPink();
                        case YELLOW -> score += agg.getYellow();
                        case GREEN -> score += agg.getGreen();
                        case BLUE -> score += agg.getBlue();
                        case SILVER -> score += agg.getSilver();
                    }
                    // shape
                    switch (tip.getShape()) {
                        case SQUARE -> score += agg.getSquare();
                        case ROUND -> score += agg.getRound();
                        case ALMOND -> score += agg.getAlmond();
                        case BALLERINA -> score += agg.getBallerina();
                        case STILETTO -> score += agg.getStiletto();
                    }
                    return score;
                })
                .reduce(0.0, Double::sum);
    }

    private Mono<NailSetRecommendationDTO.NailSetDTO> convertToNailSetDTO(NailSet nailSet) {
        return groupRepository.findById(nailSet.getNailGroupId())
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "네일 그룹을 찾을 수 없습니다.")))
                .flatMap(nailGroup -> {
                    List<Integer> tipIds = List.of(
                            nailGroup.getFingerThumb(),
                            nailGroup.getFingerIndex(),
                            nailGroup.getFingerMiddle(),
                            nailGroup.getFingerRing(),
                            nailGroup.getFingerPinky()
                    );
                    return tipRepository.findAllById(tipIds)
                            .collectList()
                            .map(tips -> {
                                if (tips.size() != 5) {
                                    // 일부 Tip이 없으면 예외 처리
                                    throw new ReportableError(HttpStatus.NOT_FOUND, "5개의 NailTip을 찾을 수 없습니다.");
                                }
                                return new NailSetRecommendationDTO.NailSetDTO(
                                        nailSet.getId(),
                                        new NailSetRecommendationDTO.NailImageDTO(tips.get(0).getImageUrl()),
                                        new NailSetRecommendationDTO.NailImageDTO(tips.get(1).getImageUrl()),
                                        new NailSetRecommendationDTO.NailImageDTO(tips.get(2).getImageUrl()),
                                        new NailSetRecommendationDTO.NailImageDTO(tips.get(3).getImageUrl()),
                                        new NailSetRecommendationDTO.NailImageDTO(tips.get(4).getImageUrl())
                                );
                            });
                });
    }

    /**
     * 내부 클래스: NailSetDTO + score
     */
    private static class ScoredNailSetDTO {
        final NailSetRecommendationDTO.NailSetDTO nailSetDTO;
        final double score;

        public ScoredNailSetDTO(NailSetRecommendationDTO.NailSetDTO nailSetDTO, double score) {
            this.nailSetDTO = nailSetDTO;
            this.score = score;
        }
    }
}
