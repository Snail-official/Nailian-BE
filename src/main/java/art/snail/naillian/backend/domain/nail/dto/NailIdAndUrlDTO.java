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

    public static NailIdAndUrlDTO from(NailAssets org) {
        return new NailIdAndUrlDTO(
                org.getId(),
                org.getImageUrl()
        );
    }

    public static NailIdAndUrlDTO from(NailTip org) {
        return new NailIdAndUrlDTO(
                org.getId(),
                org.getImageUrl()
        );
    }
}
