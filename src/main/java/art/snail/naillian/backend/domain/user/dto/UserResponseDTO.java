package art.snail.naillian.backend.domain.user.dto;

import art.snail.naillian.backend.domain.user.entity.User;
import java.time.LocalDateTime;
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
                user.getOnboardingStepsBitmask() == 0x03 ? 1 : 0
        );
    }
}
