package art.snail.naillian.backend.domain.onboarding.dto;

import art.snail.naillian.backend.domain.onboarding.entity.OnboardingStep;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@AllArgsConstructor
public class OnboardingStepDTO {
    private OnboardingStep nextOnboardingStep;
}
