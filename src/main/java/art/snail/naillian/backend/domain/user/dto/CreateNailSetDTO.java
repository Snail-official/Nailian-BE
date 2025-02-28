package art.snail.naillian.backend.domain.user.dto;

import art.snail.naillian.backend.domain.nail.dto.NailIdDTO;
import lombok.Data;

import java.util.List;

@Data
public class CreateNailSetDTO {
    private NailIdDTO thumb;
    private NailIdDTO index;
    private NailIdDTO middle;
    private NailIdDTO ring;
    private NailIdDTO pinky;

    public List<Integer> toList() {
        return List.of(thumb.getId(), index.getId(), middle.getId(), ring.getId(), pinky.getId());
    }
}
