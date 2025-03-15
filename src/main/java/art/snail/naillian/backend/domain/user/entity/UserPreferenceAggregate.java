package art.snail.naillian.backend.domain.user.entity;

import art.snail.naillian.backend.domain.nail.dto.NailIdAndUrlDTO;
import art.snail.naillian.backend.domain.nail.service.NailService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_preference_aggregate")
public class UserPreferenceAggregate {
    @Id
    private Long id;

    private Integer userId;

    private int oneColor;
    private int french;
    private int gradient;
    private int art;

    private int white;
    private int black;
    private int beige;
    private int pink;
    private int yellow;
    private int green;
    private int blue;
    private int silver;

    private int square;
    private int round;
    private int almond;
    private int ballerina;
    private int stiletto;

    public Flux<NailIdAndUrlDTO> toNailIdAndUrlDTOList(NailService nailService) {
        List<Integer> categoryIndices = new ArrayList<>();
        if (oneColor > 0) categoryIndices.add(0);
        if (french > 0) categoryIndices.add(1);
        if (gradient > 0) categoryIndices.add(2);
        if (art > 0) categoryIndices.add(3);

        List<Integer> colorIndices = new ArrayList<>();
        if (white > 0) colorIndices.add(0);
        if (black > 0) colorIndices.add(1);
        if (beige > 0) colorIndices.add(2);
        if (pink > 0) colorIndices.add(3);
        if (yellow > 0) colorIndices.add(4);
        if (green > 0) colorIndices.add(5);
        if (blue > 0) colorIndices.add(6);
        if (silver > 0) colorIndices.add(7);

        List<Integer> shapeIndices = new ArrayList<>();
        if (square > 0) shapeIndices.add(0);
        if (round > 0) shapeIndices.add(1);
        if (almond > 0) shapeIndices.add(2);
        if (ballerina > 0) shapeIndices.add(3);
        if (stiletto > 0) shapeIndices.add(4);

        return nailService.findByCategoryColorShape(categoryIndices, colorIndices, shapeIndices)
                .map(tip -> new NailIdAndUrlDTO(tip.getId(), tip.getImageUrl()));
    }

}
