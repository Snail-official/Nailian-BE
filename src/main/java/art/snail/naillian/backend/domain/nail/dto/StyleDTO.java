package art.snail.naillian.backend.domain.nail.dto;

import art.snail.naillian.backend.domain.nail.entity.Style;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StyleDTO {
    private int id;
    private String name;

    public static StyleDTO fromEunm(Style style){
        return new StyleDTO(style.getId(), style.getName());
    }
}
