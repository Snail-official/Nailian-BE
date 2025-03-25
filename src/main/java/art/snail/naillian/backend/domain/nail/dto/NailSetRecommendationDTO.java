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
    private List<NailSetEmbedDTO<NailImageUrlDTO>> nailSets;

    public NailSetRecommendationDTO(Long id, String name, List<NailSetEmbedDTO<NailImageUrlDTO>> nailSets) {
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
}
