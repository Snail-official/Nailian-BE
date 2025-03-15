package art.snail.naillian.backend.domain.user.service;

import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.nail.dto.NailIdAndUrlDTO;
import art.snail.naillian.backend.domain.nail.service.NailService;
import art.snail.naillian.backend.domain.user.dto.SaveNailPreferencesDTO;
import art.snail.naillian.backend.domain.user.entity.UserPreferenceAggregate;
import art.snail.naillian.backend.domain.user.repository.UserPreferenceAggregateRepository;
import art.snail.naillian.backend.domain.user.repository.UserPreferenceRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserNailPreferenceService {

    private final UserPreferenceAggregateRepository userPreferenceAggregateRepository;
    private final NailService nailService;
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * 사용자가 선택한 네일 스타일 ID 배열을 받아, 해당 NailTip의 속성(enum)별로 선호도를 업데이트한다.
     * 온보딩 단계에서는 기존 선호 데이터를 초기화한 후, 선택된 NailTip의 각 속성에 대해 +1씩 점수를 부여합니다.
     */
    public Mono<String> saveUserPreferences(int userId, SaveNailPreferencesDTO dto) {
        List<Integer> preferenceIds = dto.getPreferences();
        if (preferenceIds.size() < 3) {
            return Mono.error(new ReportableError(HttpStatus.UNPROCESSABLE_ENTITY, "최소 3개 이상의 네일을 선택해야 합니다."));
        }
        if (preferenceIds.size() > 10) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "최대 10개까지 선택할 수 있습니다."));
        }

        return nailService.findAllById(preferenceIds)
                .collectList()
                .flatMap(tips -> {
                    if (tips.size() != preferenceIds.size()) {
                        return Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "일부 네일 스타일을 찾을 수 없습니다."));
                    }

                    return userPreferenceAggregateRepository.findByUserId(userId)
                            .flatMap(existingAggregate -> {
                                // 기존 데이터 업데이트
                                tips.forEach(tip -> {
                                    switch (tip.getCategory()) {
                                        case ONE_COLOR -> existingAggregate.setOneColor(existingAggregate.getOneColor() + 1);
                                        case FRENCH -> existingAggregate.setFrench(existingAggregate.getFrench() + 1);
                                        case GRADIENT -> existingAggregate.setGradient(existingAggregate.getGradient() + 1);
                                        case ART -> existingAggregate.setArt(existingAggregate.getArt() + 1);
                                    }
                                    switch (tip.getColor()) {
                                        case WHITE -> existingAggregate.setWhite(existingAggregate.getWhite() + 1);
                                        case BLACK -> existingAggregate.setBlack(existingAggregate.getBlack() + 1);
                                        case BEIGE -> existingAggregate.setBeige(existingAggregate.getBeige() + 1);
                                        case PINK -> existingAggregate.setPink(existingAggregate.getPink() + 1);
                                        case YELLOW -> existingAggregate.setYellow(existingAggregate.getYellow() + 1);
                                        case GREEN -> existingAggregate.setGreen(existingAggregate.getGreen() + 1);
                                        case BLUE -> existingAggregate.setBlue(existingAggregate.getBlue() + 1);
                                        case SILVER -> existingAggregate.setSilver(existingAggregate.getSilver() + 1);
                                    }
                                    switch (tip.getShape()) {
                                        case SQUARE -> existingAggregate.setSquare(existingAggregate.getSquare() + 1);
                                        case ROUND -> existingAggregate.setRound(existingAggregate.getRound() + 1);
                                        case ALMOND -> existingAggregate.setAlmond(existingAggregate.getAlmond() + 1);
                                        case BALLERINA -> existingAggregate.setBallerina(existingAggregate.getBallerina() + 1);
                                        case STILETTO -> existingAggregate.setStiletto(existingAggregate.getStiletto() + 1);
                                    }
                                });
                                return userPreferenceAggregateRepository.save(existingAggregate);
                            })
                            .switchIfEmpty(
                                    // 데이터가 존재하지 않으면 새로 생성 후 저장
                                    userPreferenceAggregateRepository.save(new UserPreferenceAggregate(
                                            null,
                                            userId,
                                            0, 0, 0, 0,  // category 초기값
                                            0, 0, 0, 0, 0, 0, 0, 0,  // color 초기값
                                            0, 0, 0, 0, 0  // shape 초기값
                                    )).flatMap(newAggregate -> {
                                        tips.forEach(tip -> {
                                            switch (tip.getCategory()) {
                                                case ONE_COLOR -> newAggregate.setOneColor(newAggregate.getOneColor() + 1);
                                                case FRENCH -> newAggregate.setFrench(newAggregate.getFrench() + 1);
                                                case GRADIENT -> newAggregate.setGradient(newAggregate.getGradient() + 1);
                                                case ART -> newAggregate.setArt(newAggregate.getArt() + 1);
                                            }
                                            switch (tip.getColor()) {
                                                case WHITE -> newAggregate.setWhite(newAggregate.getWhite() + 1);
                                                case BLACK -> newAggregate.setBlack(newAggregate.getBlack() + 1);
                                                case BEIGE -> newAggregate.setBeige(newAggregate.getBeige() + 1);
                                                case PINK -> newAggregate.setPink(newAggregate.getPink() + 1);
                                                case YELLOW -> newAggregate.setYellow(newAggregate.getYellow() + 1);
                                                case GREEN -> newAggregate.setGreen(newAggregate.getGreen() + 1);
                                                case BLUE -> newAggregate.setBlue(newAggregate.getBlue() + 1);
                                                case SILVER -> newAggregate.setSilver(newAggregate.getSilver() + 1);
                                            }
                                            switch (tip.getShape()) {
                                                case SQUARE -> newAggregate.setSquare(newAggregate.getSquare() + 1);
                                                case ROUND -> newAggregate.setRound(newAggregate.getRound() + 1);
                                                case ALMOND -> newAggregate.setAlmond(newAggregate.getAlmond() + 1);
                                                case BALLERINA -> newAggregate.setBallerina(newAggregate.getBallerina() + 1);
                                                case STILETTO -> newAggregate.setStiletto(newAggregate.getStiletto() + 1);
                                            }
                                        });
                                        return userPreferenceAggregateRepository.save(newAggregate);
                                    })
                            );
                })
                .thenReturn("선호 취향 저장 성공");
    }



    public Mono<PageDTO<NailIdAndUrlDTO>> getUserPreferences(int userId, Pageable pageable) {
        return userPreferenceAggregateRepository.findByUserId(userId)
                .flatMapMany(aggregate -> aggregate.toNailIdAndUrlDTOList(nailService))
                .collectList()
                .map(list -> new PageDTO<>(list, pageable, list.size()))
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자 선호 데이터가 없습니다.")));
    }

}
