package art.snail.naillian.backend.domain.nail.dto;

import art.snail.naillian.backend.domain.nail.entity.NailAssets;
import art.snail.naillian.backend.domain.nail.entity.NailTip;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@AllArgsConstructor
public class NailImageUrlDTO {
    private String imageUrl;

    public static NailImageUrlDTO from(NailAssets org) {
        return new NailImageUrlDTO(org.getImageUrl());
    }

    public static NailImageUrlDTO from(NailTip org) {
        return new NailImageUrlDTO(org.getImageUrl());
    }
}
