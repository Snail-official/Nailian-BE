package art.snail.naillian.backend.domain.nail;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.common.PageDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/nails")
public class NaiLController {
    @GetMapping("/")
    public Mono<CommonResponse<Object>> getNails(Pageable page) {
        return Flux.empty()
                .collectList()
                .map(list -> new PageDTO<>(list, page, list.size()))
                .map(CommonResponse::success);
    }
}
