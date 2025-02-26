package art.snail.naillian.backend.domain.nail.entity;

import art.snail.naillian.backend.domain.nail.common.NailAssetType;
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
@Table("nail_assets")
public class NailAssets {
    @Id
    private Integer id;

    private NailShape shape;
    private NailAssetType assetType;

    private String imageUrl;
    private Integer uploadedBy;
    private Boolean isDownloaded;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}
