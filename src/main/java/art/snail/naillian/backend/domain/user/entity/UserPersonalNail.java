package art.snail.naillian.backend.domain.user.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@Table("user_personal_nail")
public class UserPersonalNail {
    @Id
    private Integer id;
    private Integer userId;
    private Integer variantId;
}
