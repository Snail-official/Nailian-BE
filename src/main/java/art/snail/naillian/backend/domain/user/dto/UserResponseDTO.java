package art.snail.naillian.backend.domain.user.dto;

import art.snail.naillian.backend.domain.user.entity.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDTO {
    private Integer userId;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime createdAt;

    public static UserResponseDTO from(User user){
        return UserResponseDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
