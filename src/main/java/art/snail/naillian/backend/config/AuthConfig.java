package art.snail.naillian.backend.config;

import art.snail.naillian.backend.domain.auth.jwt.JwtAuthFilter;
import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class AuthConfig {
    private static final String[] PUBLIC_ROUTES = {
            "/",
    };

    @Bean
    SecurityWebFilterChain defaultSecurityFilterChain(
            ServerHttpSecurity http,
            JwtProvider jwtProvider
    ) throws Exception {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)  //
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())  // stateless authentication
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/auth/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(new JwtAuthFilter(jwtProvider), SecurityWebFiltersOrder.HTTP_BASIC)
                .build();
    }
}
