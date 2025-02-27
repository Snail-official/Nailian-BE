package art.snail.naillian.backend.domain.onboarding.entity;

import lombok.Getter;

@Getter
public enum OnboardingStep {
    NICKNAME(0, 1),
    PREFERENCES(1, 2),
    ;

    private final int bitmask;
    private final int requiredVersion;

    OnboardingStep(int order, int requiredVersion) {
        this.bitmask = 1 << order;
        this.requiredVersion = requiredVersion;
    }
}
