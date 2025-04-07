package art.snail.naillian.backend.domain.user.entity;

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
@Table("social_login")
public class SocialLogin {
    @Id
    private Integer id;
    private Integer userId;
    private SocialPlatform platform;
    private String platformUserId;
}
