package art.snail.naillian.backend.domain.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;


@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {
    private final JwtProvider jwtProvider;

    private static Mono<String> extractToken(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    private static Mono<String> extractTokenFromHeader(String headerValue) {
        if (headerValue == null) return Mono.empty();

        int index = headerValue.indexOf(" ");
        if (index == -1) return Mono.empty();

        return Mono.just(headerValue.substring(index + 1));
    }

    @Override
    @SuppressWarnings("all")
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.justOrEmpty(exchange)
                .flatMap(JwtAuthFilter::extractToken)
                .flatMap(JwtAuthFilter::extractTokenFromHeader)
                .flatMap(jwtProvider::authUserByToken)
                .flatMap(payload -> chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(payload)))
                .switchIfEmpty(chain.filter(exchange));
    }
}
