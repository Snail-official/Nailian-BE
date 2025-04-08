package art.snail.naillian.backend.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppleAuthRequest {
    private String identityToken;
    private String authorizationCode;
    private AppleUserDTO user;
}
