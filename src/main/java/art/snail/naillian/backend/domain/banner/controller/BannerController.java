package art.snail.naillian.backend.domain.banner.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.banner.dto.BannerResponse;
import art.snail.naillian.backend.domain.banner.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
public class BannerController {
    private final BannerService bannerService;

    @GetMapping("/home")
    public Mono<CommonResponse<List<BannerResponse>>> getHomeBanners() {
        return bannerService.getHomeBannersResponse();
    }
}
