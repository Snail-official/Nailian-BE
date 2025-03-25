package art.snail.naillian.backend.domain.nail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Getter
@AllArgsConstructor
public class NailSetRecommendationDTO {
    private StyleDTO style;
    private List<NailSetDTO> nailSets;

    public NailSetRecommendationDTO(Long id, String name, List<NailSetDTO> nailSets) {
        this.style = new StyleDTO(id, name);
        this.nailSets = nailSets;
    }

    @Data
    @Getter
    @AllArgsConstructor
    public static class StyleDTO {
        private Long id;
        private String name;
    }

    @Data
    @Getter
    @AllArgsConstructor
    public static class NailImageDTO {
        private String imageUrl;
    }

    @Data
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
}
