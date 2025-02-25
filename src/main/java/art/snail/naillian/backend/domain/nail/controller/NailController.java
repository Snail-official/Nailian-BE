package art.snail.naillian.backend.domain.nail.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.nail.dto.NailIdAndUrlDTO;
import art.snail.naillian.backend.domain.nail.dto.SaveNailPreferencesDTO;
import art.snail.naillian.backend.domain.nail.service.NailService;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/nails")
@RequiredArgsConstructor
public class NailController {
    private final NailService nailService;

    @GetMapping("/")
    public Mono<CommonResponse<Page<NailIdAndUrlDTO>>> getNails(Pageable page) {
        return nailService.getNailTips(page)
                .map(NailIdAndUrlDTO::from)
                .collectList()
                .map(list -> new PageDTO<>(list, page, list.size()))
                .map(CommonResponse::success);
    }

    @GetMapping("/preferences")
    public Mono<CommonResponse<Iterable<NailIdAndUrlDTO>>> getNailsInPreferences(
            @RequestHeader("Authorization") String authToken
    ) {
        throw new ReportableError(HttpStatus.SERVICE_UNAVAILABLE, "not yet implemented");
    }

    @PostMapping("/preferences")
    public Mono<CommonResponse<String>> saveNailPreferences(
            @RequestHeader("Authorization") String authToken,
            @RequestBody SaveNailPreferencesDTO dto
    ) {
        throw new ReportableError(HttpStatus.SERVICE_UNAVAILABLE, "not yet implemented");
    }
}
