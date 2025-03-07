package art.snail.naillian.backend.domain.nail.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
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

