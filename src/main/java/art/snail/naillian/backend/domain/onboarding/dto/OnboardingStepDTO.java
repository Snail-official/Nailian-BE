package art.snail.naillian.backend.domain.onboarding.dto;

import art.snail.naillian.backend.domain.onboarding.entity.OnboardingStep;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Data
@Getter
@AllArgsConstructor
public class OnboardingStepDTO {
    @Nullable
    private OnboardingStep nextOnboardingStep;
}
