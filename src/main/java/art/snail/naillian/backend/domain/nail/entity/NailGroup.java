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
    private Long id;

    private Long fingerThumb;
    private Long fingerIndex;
    private Long fingerMiddle;
    private Long fingerRing;
    private Long fingerPinky;
}
