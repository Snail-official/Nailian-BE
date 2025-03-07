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
    public Mono<CommonResponse<PageDTO<NailIdAndUrlDTO>>> getNailPreferences(Pageable pageable) {
        return nailService.getNailTips(pageable)  // 모든 NailTip 데이터를 Pageable 기준으로 조회
                .map(NailIdAndUrlDTO::from)
                .collectList()
                .map(list -> new PageDTO<>(list, pageable, list.size()))
                .map(CommonResponse::success);
    }

    @PostMapping("/preferences")
    public Mono<CommonResponse<String>> saveNailPreferences(
            UserAuthByTokenPayload payload,
            @RequestBody SaveNailPreferencesDTO dto
    ) {
        return nailService.saveNailPreferences(payload.getUserId(), dto)
                .then(Mono.just(CommonResponse.success("선호 취향 저장 성공", "네일 취향 저장 완료")));
    }
}
