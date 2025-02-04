package art.snail.naillian.backend.config;

import art.snail.naillian.backend.common.CommonController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouteConfig {
    @Autowired
    private CommonController commonController;

    @Bean
    public RouterFunction<ServerResponse> route() {
        return RouterFunctions.route()
                .GET("/", commonController::streamHelloWorld)
                .build();
    }
}
