package art.snail.naillian.backend.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AppleUserDTO {
    private final String name;
    private final String email;
}
