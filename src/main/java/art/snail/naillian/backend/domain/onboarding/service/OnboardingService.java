package art.snail.naillian.backend.domain.onboarding.service;

import art.snail.naillian.backend.domain.onboarding.entity.OnboardingStep;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.service.UserService;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final UserService userService;
    /**
     * 유저 ID를 받아 다음 온보딩 스텝 결정
     * DB에서 유저 조회
     * 비트마스크 분석 후 다음 스텝 결정
     */
    public Mono<OnboardingStep> getNextOnboardingStep(int userId, int maxSupportedVersion) {
        return userService.getUserById(userId)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다.")))
                .flatMap(user -> Mono.justOrEmpty(Optional.ofNullable(calculateNextStep(user, maxSupportedVersion))));
    }

    @Nullable
    private OnboardingStep calculateNextStep(User user, int maxSupportedVersion) {
        for (OnboardingStep step : OnboardingStep.values()) {
            if (maxSupportedVersion >= step.getRequiredVersion() && needsOnboarding(user, step))
                return step;
        }

        return null;
    }

    public static boolean needsOnboarding(User user, OnboardingStep step) {
        return 0 == (user.getOnboardingStepsBitmask() & step.getBitmask());
    }

    /**
     * User entity 의 bitmask 를 수정하고 수정했는지 여부를 반환합니다.
     *
     * @return 비트마스크를 변경한 경우 true, 원래 해당 온보딩을 진행해서 비트마스크에 변화가 없으면 false
     */
    public static boolean markOnboardingComplete(User user, OnboardingStep step) {
        if (!needsOnboarding(user, step))
            return false;

        user.setOnboardingStepsBitmask(user.getOnboardingStepsBitmask() | step.getBitmask());
        return true;
    }
}
