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
}
