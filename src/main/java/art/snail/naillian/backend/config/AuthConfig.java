package art.snail.naillian.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class AuthConfig {
    private static final String[] PUBLIC_ROUTES = {
            "/",
    };

    @Bean
    SecurityWebFilterChain defaultSecurityFilterChain(ServerHttpSecurity http) throws Exception {
        return http
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }
}
