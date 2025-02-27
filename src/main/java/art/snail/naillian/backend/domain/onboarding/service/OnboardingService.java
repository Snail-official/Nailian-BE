package art.snail.naillian.backend.domain.onboarding.service;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.onboarding.entity.OnboardingStep;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.service.UserService;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final UserService userService;

    private static final int STEP_NICKNAME = 0x01;
    private static final int STEP_PREFERENCES = 0x02;

    /**
     * 유저 ID를 받아 다음 온보딩 스텝 결정
     * DB에서 유저 조회
     * 비트마스크 분석 후 다음 스텝 결정
     */
    public Mono<CommonResponse<String>> getNextOnboardingStep(int userId, int maxSupportedVersion) {
        return userService.getUserById(userId)
                .flatMap(user -> {
                    OnboardingStep nextStep = calculateNextStep(user, maxSupportedVersion);
                    return Mono.just(CommonResponse.success(nextStep.name()));
                })
                .switchIfEmpty(Mono.just(CommonResponse.fail(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다.")));
    }


    private OnboardingStep calculateNextStep(User user, int maxSupportedVersion) {
        for (OnboardingStep step : OnboardingStep.values()) {
            if (maxSupportedVersion >= step.getRequiredVersion() && need(user, step))
                return step;
        }

        throw new ReportableError(HttpStatus.NO_CONTENT, "이미 모든 온보딩을 완료했습니다.");
    }

    private boolean need(User user, OnboardingStep step) {
        return 0 == (user.getOnboardingStepsBitmask() & step.getBitmask());
    }
}
