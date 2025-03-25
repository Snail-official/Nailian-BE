package art.snail.naillian.backend.domain.nail.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.nail.dto.NailImageUrlDTO;
import art.snail.naillian.backend.domain.nail.dto.NailSetEmbedDTO;
import art.snail.naillian.backend.domain.nail.dto.NailSetRecommendationDTO;
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
    public Mono<CommonResponse<Page<NailSetEmbedDTO<NailImageUrlDTO>>>> getNailSetSimilar(
            @PathVariable("id") Long id,
            @RequestParam("style") int style,
            Pageable page
    ) {
        throw new ReportableError(HttpStatus.SERVICE_UNAVAILABLE, "not yet implemented");
    }

    @GetMapping("/recommendations")
    public Mono<CommonResponse<Iterable<NailSetRecommendationDTO>>> getNailSetRecommendations(
            @RequestHeader("Authorization") String authToken
    ) {
        throw new ReportableError(HttpStatus.SERVICE_UNAVAILABLE, "not yet implemented");
    }

    @GetMapping("/feed")
    public Mono<CommonResponse<PageDTO<NailSetEmbedDTO<NailImageUrlDTO>>>> getNailSetFeed(
            @RequestParam("style") int style,
            Pageable page
    ) {
        return nailService.getNailSetFeed(style, page)
                .map(pageDTO -> CommonResponse.success(pageDTO, "네일 세트 피드 조회 성공"));
    }
}
