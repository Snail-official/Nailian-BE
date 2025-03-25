package art.snail.naillian.backend.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ModelDetail {
    @JsonProperty("releaseDate")
    private String releaseDate;
    private String url;
}
