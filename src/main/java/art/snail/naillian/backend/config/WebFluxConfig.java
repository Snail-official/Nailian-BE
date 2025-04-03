package art.snail.naillian.backend.config;

import art.snail.naillian.backend.page.ReactiveCursorBasedPageableHandlerMethodArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {
    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(new ReactiveCursorBasedPageableHandlerMethodArgumentResolver());
    }
}
