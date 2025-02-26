package art.snail.naillian.backend.domain.banner.service;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.banner.dto.BannerResponse;
import art.snail.naillian.backend.domain.banner.repository.BannerRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;

    public Mono<List<BannerResponse>> getHomeBanners(){
        return bannerRepository.findAll()
                .map(banner -> new BannerResponse(banner.getId(), banner.getImageUrl(), banner.getLink()))
                .collectList()
                .flatMap(list -> list.isEmpty()
                        ? Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "배너 데이터를 찾을 수 없습니다."))
                        : Mono.just(list)
                );
    }

    public Mono<CommonResponse<List<BannerResponse>>> getHomeBannersResponse() {
        return getHomeBanners()
                .map(list -> CommonResponse.<List<BannerResponse>>success(list));
    }
}
