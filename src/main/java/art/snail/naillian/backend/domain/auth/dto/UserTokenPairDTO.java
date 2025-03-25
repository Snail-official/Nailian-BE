package art.snail.naillian.backend.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserTokenPairDTO {
    private String accessToken;
    private String refreshToken;
}
