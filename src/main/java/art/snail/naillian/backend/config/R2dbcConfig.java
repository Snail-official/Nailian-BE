package art.snail.naillian.backend.config;

import art.snail.naillian.backend.page.CursorSupportingR2dbcRepositoryFactoryBean;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.lang.NonNull;

@Configuration
@EnableR2dbcRepositories(
        repositoryFactoryBeanClass = CursorSupportingR2dbcRepositoryFactoryBean.class
)
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    @Override
    @NonNull
    public ConnectionFactory connectionFactory() {
        throw new RuntimeException("Cannot acquire ConnectionFactory (All strategies tried)");
    }
}
