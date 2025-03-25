package art.snail.naillian.backend.domain.user.entity;

import art.snail.naillian.backend.domain.nail.common.NailCategory;
import art.snail.naillian.backend.domain.nail.common.NailColor;
import art.snail.naillian.backend.domain.nail.common.NailShape;
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

    private Integer userId;
    private double shape;
    private double color;
    private double category;

    public UserPreferences(Integer id, Integer userId, NailShape nailShape, NailColor nailColor, NailCategory nailCategory){
        this.id = id;
        this.userId = userId;
        this.shape = nailShape.ordinal();
        this.color = nailColor.ordinal();
        this.category = nailCategory.ordinal();
    }

}
