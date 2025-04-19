package art.snail.naillian.backend.domain.user.controller;


import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.auth.jwt.UserAuthByTokenPayload;
import art.snail.naillian.backend.domain.nail.dto.NailImageUrlDTO;
import art.snail.naillian.backend.domain.nail.dto.NailSetEmbedDTO;
import art.snail.naillian.backend.domain.user.dto.*;
import art.snail.naillian.backend.domain.user.service.UserService;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    /**
     * 사용자 조회
     */
    @GetMapping("/me")
    public Mono<CommonResponse<UserResponseDTO>> getUserInfo(
            UserAuthByTokenPayload payload
    ) {
        return userService.getUserById(payload.getUserId())
                .map(UserResponseDTO::from)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")))
                .map(userResponse -> CommonResponse.success(userResponse, "사용자 정보 조회 성공"));
    }

    /**
     * 닉네임 변경 (온보딩 여부 반영)
     */
    @PatchMapping("/me/nickname")
    public Mono<CommonResponse<UserResponseDTO>> updateNickname(
            UserAuthByTokenPayload payload,
            @RequestBody UserChangeNicknameDTO requestBody
    ) {
        return userService.updateNickname(payload.getUserId(), requestBody.getNickname())
                .flatMap(tuple -> {
                    UserResponseDTO dto = UserResponseDTO.from(tuple.getT1());
                    String message = tuple.getT2();
                    return Mono.just(CommonResponse.success(dto, message));
                });
    }

    /**
     * 회원 탈퇴(논리적 삭제)
     */
    @DeleteMapping("/me")
    public Mono<CommonResponse<Void>> deleteUser(
            UserAuthByTokenPayload payload
    ) {
        return userService.deleteUser(payload.getUserId())
                .then(Mono.just(CommonResponse.success(null, "회원 탈퇴가 완료되었습니다.")));
    }

    /**
     * 사용자의 네일 세트 조회
     */
    @GetMapping("/me/nail-sets")
    public Mono<CommonResponse<Page<NailSetEmbedDTO<NailImageUrlDTO>>>> getUserNailSets(
            UserAuthByTokenPayload payload,
            Pageable page
    ) {
        return userService.getUserNailSet(payload.getUserId(), page)
                .map(dto -> CommonResponse.success(dto, "사용자 네일 세트 조회 성공"));
    }

    /**
     * 사용자의 네일 세트 생성
     */
    @PostMapping("/me/nail-sets")
    public Mono<CommonResponse<NailSetEmbedDTO<NailImageUrlDTO>>> createUserNailSet(
            UserAuthByTokenPayload payload,
            @RequestBody CreateNailSetDTO requestBody
    ) {
        return userService.createUserNailSet(payload.getUserId(), requestBody.toList())
                .map(tipEmbedDto -> tipEmbedDto.transform((tip) -> new NailImageUrlDTO(tip.getImageUrl())))
                .map(CommonResponse::success);
    }

    /**
     * 사용자 보관함에 네일 세트 저장
     */
    @PostMapping("/me/nail-sets/save")
    public Mono<CommonResponse<NailSetEmbedDTO<NailImageUrlDTO>>> scrapNailSet(
            UserAuthByTokenPayload payload,
            @RequestBody UserScrapingNailSetIdDTO requestBody
    ) {
        return userService.scrapNailSetForUser(payload.getUserId(), requestBody.getNailSetId())
                .map(tipEmbedDto -> tipEmbedDto.transform((tip) -> new NailImageUrlDTO(tip.getImageUrl())))
                .map(CommonResponse::success);
    }

    /**
     * 사용자 보관함에서 네일 세트 삭제
     */
    @DeleteMapping("/me/nail-sets/{id}")
    public Mono<CommonResponse<Void>> unScrapNailSet(
            UserAuthByTokenPayload payload,
            @PathVariable("id") int setId
    ) {
        return userService.unScrapNailSetForUser(payload.getUserId(), setId)
                .then(Mono.just(CommonResponse.success(null, "네일 세트가 보관함에서 삭제되었습니다.")));
    }

    /**
     * 사용자가 이벤트 응모
     */
    @PostMapping("/me/event")
    public Mono<CommonResponse<Void>> submitEvent(
            UserAuthByTokenPayload payload,
            @RequestBody EventSubmissionDTO body
    ) {
        return userService.submitEvent(payload.getUserId(), body)
                .thenReturn(CommonResponse.success(null, "응모가 완료되었습니다."));
    }

    /**
     * 이벤트 응모 여부 조회
     */
    @GetMapping("/me/event")
    public Mono<CommonResponse<Boolean>> checkEventStatus(
            UserAuthByTokenPayload payload
    ) {
        return userService.hasEnrolledEvent(payload.getUserId())
                .map(flag -> CommonResponse.success(flag, "이벤트 응모 여부 조회 성공"));
    }
}
