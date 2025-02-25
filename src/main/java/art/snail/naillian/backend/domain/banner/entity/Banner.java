package art.snail.naillian.backend.domain.banner.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("banner")
public class Banner {
    @Id
    private Long id;

    private String imageUrl;
    private String link;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
