package art.snail.naillian.backend.domain.auth.jwt;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

@Getter
public class UserAuthByTokenPayload extends AbstractAuthenticationToken {
    private final Integer userId;
    private final String tokenType;
    private final String accessToken;

    public UserAuthByTokenPayload(Integer userId, String tokenType, String accessToken) {
        super(null);
        this.userId = userId;
        this.tokenType = tokenType;
        this.accessToken = accessToken;
    }

    @Override
    public Object getCredentials() {
        return this.tokenType;
    }

    @Override
    public Object getPrincipal() {
        return this.userId;
    }
}
