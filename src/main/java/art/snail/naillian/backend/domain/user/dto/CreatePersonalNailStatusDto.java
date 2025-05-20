package art.snail.naillian.backend.domain.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreatePersonalNailStatusDto {
    private List<Integer> steps;
}
