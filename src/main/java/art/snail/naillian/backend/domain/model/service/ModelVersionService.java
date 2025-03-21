package art.snail.naillian.backend.domain.model.service;

import art.snail.naillian.backend.common.S3Service;
import art.snail.naillian.backend.domain.model.dto.ModelVersionDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ModelVersionService {
    private final S3Service s3Service;

    public Mono<ModelVersionDTO> getLatestModelVersion() {
        return s3Service.getModelMetadata();
    }
}
