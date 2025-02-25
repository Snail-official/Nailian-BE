package art.snail.naillian.backend.domain.nail.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.nail.dto.NailImageUrlDTO;
import art.snail.naillian.backend.domain.nail.dto.NailSetEmbedDTO;
import art.snail.naillian.backend.domain.nail.service.NailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/nail-sets")
@RequiredArgsConstructor
public class NailSetController {
    private final NailService nailService;

    @GetMapping("/{id}")
    public Mono<CommonResponse<NailSetEmbedDTO<NailImageUrlDTO>>> getNailSet(
            @PathVariable("id") Long id
    ) {
        return nailService.getNailsBySetId(id)
                .map(NailImageUrlDTO::from)
                .collectList()
                .map(list -> new NailSetEmbedDTO<>(id, list))
                .map(CommonResponse::success);
    }

}
