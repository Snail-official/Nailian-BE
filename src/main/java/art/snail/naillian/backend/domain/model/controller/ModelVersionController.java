package art.snail.naillian.backend.domain.model.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.model.dto.ModelVersionDTO;
import art.snail.naillian.backend.domain.model.service.ModelVersionService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/model")
@RequiredArgsConstructor
public class ModelVersionController {

    private final ModelVersionService modelVersionService;

    @GetMapping("/version")
    public Mono<CommonResponse<ModelVersionDTO>> getLatestModelVersion() {
        return modelVersionService.getLatestModelVersion()
                .map(dto -> CommonResponse.success(dto, "최신 모델 정보 조회 성공"))
                .defaultIfEmpty(CommonResponse.fail(HttpStatus.NOT_FOUND, "최신 모델 정보를 찾을 수 없습니다."));
    }
}
