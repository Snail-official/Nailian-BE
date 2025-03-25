package art.snail.naillian.backend.domain.nail.dto;

import art.snail.naillian.backend.domain.nail.entity.NailAssets;
import art.snail.naillian.backend.domain.nail.entity.NailTip;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NailIdAndUrlDTO {
    private int id;
    private String imageUrl;
    private String shape;

    public static NailIdAndUrlDTO from(NailAssets org) {
        return new NailIdAndUrlDTO(
                org.getId(),
                org.getImageUrl(),
                org.getShape() != null ? org.getShape().name() : null
        );
    }

    public static NailIdAndUrlDTO from(NailTip org) {
        return new NailIdAndUrlDTO(
                org.getId(),
                org.getImageUrl(),
                org.getShape() != null ? org.getShape().name() : null
        );
    }
}
