package art.snail.naillian.backend.domain.auth.utils;

import art.snail.naillian.backend.domain.auth.jwt.UserAuthByTokenPayload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

public class AuthUtil {
    public static Mono<UserAuthByTokenPayload> getContextUser(Mono<SecurityContext> ctx) {
        return ctx.map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .cast(UserAuthByTokenPayload.class);
    }
}
