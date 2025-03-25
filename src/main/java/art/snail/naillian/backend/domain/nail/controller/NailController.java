package art.snail.naillian.backend.domain.nail.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.auth.jwt.UserAuthByTokenPayload;
import art.snail.naillian.backend.domain.nail.dto.NailIdAndUrlDTO;
import art.snail.naillian.backend.domain.nail.dto.SaveNailPreferencesDTO;
import art.snail.naillian.backend.domain.nail.service.NailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/nails")
@RequiredArgsConstructor
public class NailController {
    private final NailService nailService;

    @GetMapping("/")
    public Mono<CommonResponse<Page<NailIdAndUrlDTO>>> getNails(
            @RequestParam(value = "shape", required = false) String shape,
            @RequestParam(value = "color", required = false) String color,
            @RequestParam(value = "category", required = false) String category,
            Pageable page
    ) {
        return nailService.getNailTipsByAttributes(shape, color, category, page)
                .map(CommonResponse::success);
    }

    @GetMapping("/preferences")
    public Mono<CommonResponse<PageDTO<NailIdAndUrlDTO>>> getUserNailPreferences(
            UserAuthByTokenPayload payload,
            Pageable page) {

        return nailService.getUserNailPreferences(payload.getUserId(), page)
                .map(pageDTO -> {
                    List<NailIdAndUrlDTO> dtos = pageDTO.getContent().stream()
                            .map(NailIdAndUrlDTO::from)
                            .collect(Collectors.toList());
                    return new PageDTO<>(dtos, pageDTO.getPageable(), pageDTO.getTotalElements());
                })
                .map(CommonResponse::success);
    }

    @PostMapping("/preferences")
    public Mono<CommonResponse<Void>> saveNailPreferences(
            UserAuthByTokenPayload payload,
            @RequestBody SaveNailPreferencesDTO dto
    ) {
        return nailService.saveNailPreferences(payload.getUserId(), dto)
                .then(Mono.just(CommonResponse.success(null, "선호 취향 저장 성공")));
    }
}
