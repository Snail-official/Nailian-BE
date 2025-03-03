package art.snail.naillian.backend.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModelVersionDTO {

    private String latestModel;
    private String downloadUrl;
}
