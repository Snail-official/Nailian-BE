package art.snail.naillian.backend.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AppleAuthRequest {
    private final String identityToken;
    private final String authorizationCode;
    private final AppleUserDTO user;
}
