package art.snail.naillian.backend.domain.user.entity;

import art.snail.naillian.backend.domain.nail.entity.NailTip;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_preferences")
public class UserPreferences {
    @Id
    private Integer id;

    private Integer userId;
    private Integer tipId;

    public UserPreferences(Integer userId, NailTip tip) {
        this.userId = userId;
        this.tipId = tip.getId();
    }
}
