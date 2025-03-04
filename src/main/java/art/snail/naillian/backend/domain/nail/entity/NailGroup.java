package art.snail.naillian.backend.domain.nail.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Table("nail_group")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NailGroup {
    @Id
    private Integer id;

    private Integer fingerThumb;
    private Integer fingerIndex;
    private Integer fingerMiddle;
    private Integer fingerRing;
    private Integer fingerPinky;

    public static NailGroup fromList(List<Integer> tipIds) {
        return builder()
                .fingerThumb(tipIds.get(0))
                .fingerIndex(tipIds.get(1))
                .fingerMiddle(tipIds.get(2))
                .fingerRing(tipIds.get(3))
                .fingerPinky(tipIds.get(4))
                .build();
    }
}
