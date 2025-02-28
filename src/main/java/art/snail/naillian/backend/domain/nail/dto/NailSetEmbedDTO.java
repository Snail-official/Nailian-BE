package art.snail.naillian.backend.domain.nail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Getter
@AllArgsConstructor
public class NailSetEmbedDTO<T> {
    private Integer id;

    private T thumb;
    private T index;
    private T middle;
    private T ring;
    private T pinky;

    public NailSetEmbedDTO(Integer id, List<T> embedDataList) {
        this.id = id;
        this.thumb = embedDataList.get(0);
        this.index = embedDataList.get(1);
        this.middle = embedDataList.get(2);
        this.ring = embedDataList.get(3);
        this.pinky = embedDataList.get(4);
    }
}
