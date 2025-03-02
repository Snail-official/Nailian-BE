package art.snail.naillian.backend.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_preferences")
public class UserPreferences {
    @Id
    private Integer id;

    @Column("user_id")
    private Integer userId;
    @Column("shape")
    private double shape;
    @Column("color")
    private double color;
    @Column("category")
    private double category;

}
