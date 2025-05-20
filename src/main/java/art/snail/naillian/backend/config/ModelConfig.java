package art.snail.naillian.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cloudfront")
@Getter
@Setter
public class ModelConfig {
    private String s3ModelUrl;
    private String personalNailVariantsUrl;
}
