package art.snail.naillian.backend.domain.nail.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.auth.jwt.UserAuthByTokenPayload;
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

import java.util.List;

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

    @GetMapping("/recommendations")
    public Mono<CommonResponse<List<NailSetRecommendationDTO>>> getRecommendedNailSets(
            UserAuthByTokenPayload payload
    ) {
        return nailService.getNailSetRecommendations(payload.getUserId())
                .map(list -> CommonResponse.success(list, "추천 네일 세트 조회 성공"));
    }

    @GetMapping("/{id}/similar")
    public Mono<CommonResponse<Page<NailSetEmbedDTO<NailImageUrlDTO>>>> getNailSetSimilar(
            @PathVariable("id") Long id,
            @RequestParam("style") int style,
            Pageable page
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
