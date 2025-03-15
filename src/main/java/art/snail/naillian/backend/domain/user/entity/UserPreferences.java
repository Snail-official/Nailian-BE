package art.snail.naillian.backend.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_preferences")
public class UserPreferences {
    @Id
    private Integer id;


    private Integer nailTipId;
    private Integer userId;

    private int shape;
    private int color;
    private int category;

    // 온보딩 시에는 항상 1이고 나중에 aggregate시 합산 가능
    private int score;
}
