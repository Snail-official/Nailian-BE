package art.snail.naillian.backend.domain.onboarding.service;

import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.onboarding.dto.OnboardingStatusResponse;
import art.snail.naillian.backend.domain.onboarding.entity.OnboardingStep;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    private static final int STEP_NICKNAME = 0x01;
    private static final int STEP_PREFERENCES = 0x02;

    /**
     * 유저 ID를 받아 다음 온보딩 스텝 결정
     * DB에서 유저 조회
     * 비트마스크 분석 후 다음 스텝 결정
     */
    public Mono<OnboardingStatusResponse> getNextOnboardingStep(String authorization) {
        return extractUserId(authorization)
                .flatMap(userId -> userRepository.findById(userId)
                        .map(this::calculateNextStep)
                        .map(step -> OnboardingStatusResponse.success(step.name()))
                        .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다.")))
                );
    }

    private Mono<Integer> extractUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "JWT 토큰이 필요합니다."));
        }
        String token = authorization.substring("Bearer ".length());
        return jwtProvider.getUserIdFromToken(token);
    }

    private OnboardingStep calculateNextStep(User user) {
        int bitmask = user.getOnboardingStepsBitmask();
        if ((bitmask & STEP_NICKNAME) == 0)
            return OnboardingStep.ONBOARDING_NICKNAME;
        if ((bitmask & STEP_PREFERENCES) == 0) {
            return OnboardingStep.ONBOARDING_PREFERENCES;
        }
        throw new ReportableError(HttpStatus.BAD_REQUEST, "이미 모든 온보딩을 완료했습니다.");
    }
}
