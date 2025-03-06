package art.snail.naillian.backend.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;

    @Value("${cloudfront.cdn}")
    private String cloudFrontCdn;

    private static final String MODEL_METADATA_FILE = "model/model.json";

    /**
     * CloudFront에서 모델 메타데이터 JSON을 읽고 DTO로 변환하여 반환
     * @return ModelVersionDTO
     */
    /**
     * WebClient를 생성할 때부터 리디렉션을 자동으로 따르도록 설정
     */
    public S3Service() {
        this.webClient = WebClient.builder()
                // Reactive는 256kb 까지만 인메모리에 저장하므로 사전 인메모리 정의
                // builder를 사용해서 기본적으로 http 리다이렉션(301)을 자동으로 처리하게끔 수정
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .build();
    }

    /**
     * CloudFront에서 모델 메타데이터 JSON을 읽고 DTO로 변환하여 반환
     * @return ModelVersionDTO
     */
    public Mono<JsonNode> getModelMetadata() {
        String metadataUrl = cloudFrontCdn + "/" + MODEL_METADATA_FILE;

        return webClient.get()
                .uri(metadataUrl)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(json -> Mono.fromCallable(() -> objectMapper.readTree(json)))
                .onErrorResume(e -> Mono.empty());
    }
}