package art.snail.naillian.backend.domain.user.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table("user")
public class User {
    @Id
    private Integer id;
    private String nickname;
    private UserType userType;
    private String profileImageUrl;
    private String registeredIp;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private int onboardingStage;
    // 각 스텝을 완료했는지 저장할 수 있는 비트마스크
    /** 닉네임만 완료한 경우 0x01
     * 닉네임 + 취향선택 완료 0x01 | 0x02 = 0x03
     */
    private int onboardingStepsBitmask;
}
