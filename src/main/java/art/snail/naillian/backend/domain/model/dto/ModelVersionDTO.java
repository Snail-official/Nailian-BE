package art.snail.naillian.backend.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ModelVersionDTO {

    private String downloadUrl;
    private String releaseDate;
}
