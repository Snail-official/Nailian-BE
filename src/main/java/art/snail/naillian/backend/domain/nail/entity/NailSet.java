package art.snail.naillian.backend.domain.nail.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("nail_set")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NailSet {
    @Id
    private Integer id;

    private Integer nailGroupId;

    private String name;
    private Integer uploadedBy;

    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}
