package art.snail.naillian.backend.domain.nail.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NailSetRecommendationDTO {
    private StyleDTO style;
    private List<NailSetDTO> nailSets;

    @Getter
    @AllArgsConstructor
    public static class StyleDTO {
        private Integer id;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    public static class NailSetDTO {
        private Integer id;
        private NailImageDTO thumb;
        private NailImageDTO index;
        private NailImageDTO middle;
        private NailImageDTO ring;
        private NailImageDTO pinky;
    }

    @Getter
    @AllArgsConstructor
    public static class NailImageDTO {
        private String imageUrl;
    }
}
