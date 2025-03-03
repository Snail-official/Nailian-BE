package art.snail.naillian.backend.domain.model.service;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.common.S3Service;
import art.snail.naillian.backend.domain.model.dto.ModelVersionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ModelVersionService {
    private final S3Service s3Service;

    public Mono<CommonResponse<ModelVersionDTO>> getLatestModelVersion() {

        String latestModelFileName = "ResNet101-DUC-12-" + LocalDate.now().toString().replace("-", "") + "-int8.onnx";

        String downloadUrl = s3Service.getCloudFrontModelUrl(latestModelFileName);

        ModelVersionDTO modelVersionDTO = new ModelVersionDTO(latestModelFileName, downloadUrl);
        return Mono.just(CommonResponse.success(modelVersionDTO, "최신 모델 버전 조회 성공"));
    }
}
