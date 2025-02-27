package art.snail.naillian.backend.domain.user.dto;

import art.snail.naillian.backend.domain.onboarding.entity.OnboardingStep;
import art.snail.naillian.backend.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDTO {
    private Integer id;
    private String nickname;
    private String profileImage;
    private Integer onboardingProgress;

    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getOnboardingStepsBitmask() == OnboardingStep.ALL_STEP_BITS ? 1 : 0
        );
    }
}
