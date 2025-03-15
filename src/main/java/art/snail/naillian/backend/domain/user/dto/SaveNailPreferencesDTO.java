package art.snail.naillian.backend.domain.user.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Data
public class SaveNailPreferencesDTO {
    private List<Integer> preferences;
}
