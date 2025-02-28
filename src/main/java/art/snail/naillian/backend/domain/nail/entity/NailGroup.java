package art.snail.naillian.backend.domain.nail.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("nail_group")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NailGroup {
    @Id
    private Integer id;

    private Integer fingerThumb;
    private Integer fingerIndex;
    private Integer fingerMiddle;
    private Integer fingerRing;
    private Integer fingerPinky;
}
