package art.snail.naillian.backend.domain.nail.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("nail_set")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NailSet {
    private Long id;

    private Long nailGroupId;

    private String name;
    private Long uploadedBy;

    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}
