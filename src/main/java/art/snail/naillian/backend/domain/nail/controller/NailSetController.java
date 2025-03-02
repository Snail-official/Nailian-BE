package art.snail.naillian.backend.domain.nail.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.nail.dto.NailImageUrlDTO;
import art.snail.naillian.backend.domain.nail.dto.NailSetEmbedDTO;
import art.snail.naillian.backend.domain.nail.dto.NailSetRecommendationDTO;
import art.snail.naillian.backend.domain.nail.repository.NailSetRepository;
import art.snail.naillian.backend.domain.nail.service.NailService;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/nail-sets")
@RequiredArgsConstructor
public class NailSetController {
    private final NailService nailService;

    @GetMapping("/{id}")
    public Mono<CommonResponse<NailSetEmbedDTO<NailImageUrlDTO>>> getNailSet(
            @PathVariable("id") Integer id
    ) {
        return nailService.getNailsBySetId(id)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "네일 세트를 찾을 수 없습니다.")))
                .map(NailImageUrlDTO::from)
                .collectList()
                .map(list -> new NailSetEmbedDTO<>(id, list))
                .map(CommonResponse::success);
    }

    @GetMapping("/{id}/similar")
    public Mono<CommonResponse<PageDTO<NailSetEmbedDTO<NailImageUrlDTO>>>> getNailSetSimilar(
            @PathVariable("id") Integer id,
            @RequestParam(value = "style", required = false) Integer style,  // <-- folderId
            Pageable pageable
    ) {
        if (style == null) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "스타일(폴더 id)을 지정해야 합니다."));
        }

        return nailService.getSimilarNailSets(id, style, pageable)
                .map(pageDTO -> CommonResponse.success(pageDTO, "유사한 네일 세트 조회 성공"))
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "유사한 네일 세트를 찾을 수 없습니다.")));
    }

    @GetMapping("/recommendations")
    public Mono<CommonResponse<Iterable<NailSetRecommendationDTO>>> getNailSetRecommendations(
            @RequestHeader("Authorization") String authToken
    ) {
        throw new ReportableError(HttpStatus.SERVICE_UNAVAILABLE, "not yet implemented");
    }

    @GetMapping("/feed")
    public Mono<CommonResponse<Page<NailSetEmbedDTO<NailImageUrlDTO>>>> getNailSetFeed(
            @RequestParam("style") int style
    ) {
        throw new ReportableError(HttpStatus.SERVICE_UNAVAILABLE, "not yet implemented");
    }
}
