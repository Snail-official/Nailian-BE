package art.snail.naillian.backend.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformModel {
    private ModelDetail segmentation;
    private ModelDetail detection;
}
