package art.snail.naillian.backend.domain.nail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Getter
public class NailSetRecommendationDTO {
    private RecommendationStyleEntry style;
    private List<NailSetEmbedDTO<NailImageUrlDTO>> nailSets;

    public NailSetRecommendationDTO(
            Long id,
            String name,
            List<NailSetEmbedDTO<NailImageUrlDTO>> nailSets
    ) {
        this.style = new RecommendationStyleEntry(id, name);
        this.nailSets = nailSets;
    }

    @Data
    @Getter
    @AllArgsConstructor
    private static class RecommendationStyleEntry {
        private Long id;
        private String name;
    }
}
