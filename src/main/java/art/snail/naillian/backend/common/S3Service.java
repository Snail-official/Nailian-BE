package art.snail.naillian.backend.common;

import art.snail.naillian.backend.config.ModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final WebClient webClient;
    private final ModelConfig modelConfig;

    public Mono<JsonNode> getModelMetadata() {
        return webClient.get()
                .uri(modelConfig.getS3ModelUrl())
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(e -> Mono.empty());
    }
}