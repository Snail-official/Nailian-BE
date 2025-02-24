package art.snail.naillian.backend.domain.nail.entity;

import art.snail.naillian.backend.domain.nail.common.NailCategory;
import art.snail.naillian.backend.domain.nail.common.NailColor;
import art.snail.naillian.backend.domain.nail.common.NailShape;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("nail_tip")
public class NailTip {

    @Id
    private Integer id;
    private NailShape shape;
    private NailColor color;
    private NailCategory category;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private Integer checkedBy;
}

