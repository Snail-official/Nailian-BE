package art.snail.naillian.backend.domain.banner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BannerResponse {
    private Long id;
    private String imageUrl;
    private String link;
}
