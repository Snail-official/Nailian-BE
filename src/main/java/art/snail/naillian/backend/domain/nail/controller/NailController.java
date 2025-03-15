package art.snail.naillian.backend.domain.nail.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.auth.jwt.UserAuthByTokenPayload;
import art.snail.naillian.backend.domain.nail.dto.NailIdAndUrlDTO;
import art.snail.naillian.backend.domain.nail.service.NailService;
import art.snail.naillian.backend.domain.user.dto.SaveNailPreferencesDTO;
import art.snail.naillian.backend.domain.user.service.UserNailPreferenceService;
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
    private final UserNailPreferenceService userNailPreferenceService;

    @GetMapping("/")
    public Mono<CommonResponse<Page<NailIdAndUrlDTO>>> getNails(Pageable page) {
        return nailService.getNailTips(page)
                .map(NailIdAndUrlDTO::from)
                .collectList()
                .map(list -> new PageDTO<>(list, page, list.size()))
                .map(CommonResponse::success);
    }

    @GetMapping("/preferences")
    public Mono<CommonResponse<PageDTO<NailIdAndUrlDTO>>> getPreferences(UserAuthByTokenPayload payload,
                                                                         Pageable page) {
        return userNailPreferenceService.getUserPreferences(payload.getUserId(), page)
                .map(CommonResponse::success);
    }



    @PostMapping("/preferences")
    public Mono<CommonResponse<String>> savePreferences(UserAuthByTokenPayload payload,
                                                        @RequestBody SaveNailPreferencesDTO dto) {
        return userNailPreferenceService.saveUserPreferences(payload.getUserId(), dto)
                .map(result -> CommonResponse.success(null, result));
    }

}
