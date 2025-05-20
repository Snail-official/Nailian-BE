package art.snail.naillian.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalNailStatusDto {
    private String title;
    private String description;
    private List<String> tags;

    @JsonProperty("icon_url")
    private String iconUrl;

    @JsonProperty("background_color")
    private Integer backgroundColor;

    @JsonProperty("set_ids")
    private List<Integer> setIds;
}
