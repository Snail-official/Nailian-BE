package art.snail.naillian.backend.common;

import art.snail.naillian.backend.config.ModelConfig;
import art.snail.naillian.backend.domain.user.dto.PersonalNailStatusDto;
import art.snail.naillian.backend.errors.ReportableError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class S3Service {
    private static final Logger log = LoggerFactory.getLogger(S3Service.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    public Mono<Map<String, PersonalNailStatusDto>> getPersonalNailVariants() {
        return webClient.get()
                .uri(modelConfig.getPersonalNailVariantsUrl())
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .cache(Duration.ofHours(1))
                .map(this::parseToNailVariants)
                .onErrorResume(e -> {
                    log.warn("Failed to retrieve data", e);
                    return Mono.empty();
                })
                ;
    }

    private Map<String, PersonalNailStatusDto> parseToNailVariants(JsonNode node) {
        Map<String, PersonalNailStatusDto> map = new HashMap<>();
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            String fieldName = it.next();

            try {
                PersonalNailStatusDto dto = objectMapper.readValue(node.get(fieldName).toString(), PersonalNailStatusDto.class);
                map.put(fieldName, dto);
            } catch (Exception e) {
                throw new ReportableError(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 저장된 네일 정보가 잘못된 형식을 갖고 있습니다.");
            }
        }
        return map;
    }

    public Mono<Map<String, Integer>> getPersonalNailMapping() {
        return webClient.get()
                .uri(modelConfig.getPersonalNailMappingUrl())
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseToPersonalNailMapping)
                .cache(token -> Duration.ofMinutes(30), e -> Duration.ZERO, () -> Duration.ZERO)
                .onErrorResume(e -> {
                    log.warn("Failed to retrieve data", e);
                    return Mono.error(new ReportableError(HttpStatus.INTERNAL_SERVER_ERROR, "서버가 잘못 저장된 퍼스널 네일 진단 매핑을 갖고 있습니다."));
                })
                ;
    }

    private Map<String, Integer> parseToPersonalNailMapping(JsonNode node) {
        Map<String, Integer> map = new HashMap<>();
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            String fieldName = it.next();
            map.put(fieldName, node.get(fieldName).asInt());
        }
        return map;
    }
}