package art.snail.naillian.backend.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

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
    private String registeredIp;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    // 각 스텝을 완료했는지 저장할 수 있는 비트마스크
    /** 닉네임만 완료한 경우 0x01
     * 닉네임 + 취향선택 완료 0x01 | 0x02 = 0x03
     */
    private int onboardingStepsBitmask;

    private static final Logger log = LoggerFactory.getLogger(User.class);

    public String getProfileImageUrl() {
        log.warn("User.profileImageUrl 은 현재 스키마에 존재하지 않아 항상 빈 값을 반환합니다.");
        return "";
    }
}
