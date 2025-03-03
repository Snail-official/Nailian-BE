package art.snail.naillian.backend.domain.model.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.model.dto.ModelVersionDTO;
import art.snail.naillian.backend.domain.model.service.ModelVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/model")
@RequiredArgsConstructor
public class ModelVersionController {

    private final ModelVersionService modelVersionService;

    @GetMapping("/version")
    public Mono<CommonResponse<ModelVersionDTO>> getLatestModelVersion() {
        return modelVersionService.getLatestModelVersion();
    }
}
