package art.snail.naillian.backend.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.create();

    @Value("${cloudfront.cdn}")
    private String cloudFrontCdn;

    private static final String MODEL_METADATA_FILE = "model/model.json";

    /**
     * CloudFront에서 모델 메타데이터 JSON을 읽고 DTO로 변환하여 반환
     * @return ModelVersionDTO
     */
    public Mono<JsonNode> getModelMetadata() {
        String metadataUrl = cloudFrontCdn + "/" + MODEL_METADATA_FILE;

        return webClient.get()
                .uri(metadataUrl)
                .header("Accept", "application/json")
                .exchangeToMono(response -> {
                    if (response.statusCode().is3xxRedirection()) {
                        String location = response.headers().asHttpHeaders().getFirst("Location");
                        if (location != null) {
                            return webClient.get().uri(location)
                                    .retrieve()
                                    .bodyToMono(String.class);
                        }
                    }
                    return response.bodyToMono(String.class);
                })
                .flatMap(json -> Mono.fromCallable(() -> objectMapper.readTree(json)))
                .onErrorResume(e -> Mono.empty());
    }
}