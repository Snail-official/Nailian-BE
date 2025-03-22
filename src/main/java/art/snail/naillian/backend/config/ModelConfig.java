package art.snail.naillian.backend.config;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ModelConfig {
    private String s3ModelUrl;

    public void setS3ModelUrl(String s3ModelUrl) {
        this.s3ModelUrl = s3ModelUrl;
    }
}
