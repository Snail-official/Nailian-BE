package art.snail.naillian.backend.domain.onboarding.service;

import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.onboarding.dto.OnboardingStatusResponse;
import art.snail.naillian.backend.domain.onboarding.entity.OnboardingStep;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
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
    private final JwtProvider jwtProvider;

    private static final int STEP_NICKNAME = 0x01;
    private static final int STEP_PREFERENCES = 0x02;

    /**
     * 유저 ID를 받아 다음 온보딩 스텝 결정
     * DB에서 유저 조회
     * 비트마스크 분석 후 다음 스텝 결정
     */
    public Mono<OnboardingStatusResponse> getNextOnboardingStep(int userId, int maxSupportedVersion) {
        return userService.getUserById(userId)
                .flatMap(user -> {
                    OnboardingStep nextStep = calculateNextStep(user, maxSupportedVersion);
                    return Mono.just(OnboardingStatusResponse.success(nextStep.name()));
                })
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다.")));
    }

    private OnboardingStep calculateNextStep(User user, int maxSupportedVersion) {
        if (needsNickname(user) && maxSupportedVersion >= 1) {
            return OnboardingStep.OnboardingNickname;
        }
        if (needsPreferences(user) && maxSupportedVersion >= 2) {
            return OnboardingStep.OnboardingPreferences;
        }
        throw new ReportableError(HttpStatus.NO_CONTENT, "이미 모든 온보딩을 완료했습니다.");
    }

    private boolean needsNickname(User user) {
        return (user.getOnboardingStepsBitmask() & STEP_NICKNAME) == 0;
    }

    private boolean needsPreferences(User user) {
        return (user.getOnboardingStepsBitmask() & STEP_PREFERENCES) == 0;
    }
}
