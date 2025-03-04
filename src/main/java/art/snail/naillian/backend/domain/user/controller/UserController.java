package art.snail.naillian.backend.domain.user.controller;


import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.auth.jwt.UserAuthByTokenPayload;
import art.snail.naillian.backend.domain.nail.dto.NailImageUrlDTO;
import art.snail.naillian.backend.domain.nail.dto.NailSetEmbedDTO;
import art.snail.naillian.backend.domain.user.dto.CreateNailSetDTO;
import art.snail.naillian.backend.domain.user.dto.UserChangeNicknameDTO;
import art.snail.naillian.backend.domain.user.dto.UserResponseDTO;
import art.snail.naillian.backend.domain.user.service.UserService;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
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

/** 회원 탈퇴(논리적 삭제) */
@DeleteMapping("/me")
public Mono<CommonResponse<Void>> deleteUser(
        UserAuthByTokenPayload payload
) {
    return userService.deleteUser(payload.getUserId())
            .then(Mono.just(CommonResponse.success(null, "회원 탈퇴가 완료되었습니다.")));
}

/** 사용자의 네일 세트 조회 */
@GetMapping("/me/nail-sets")
public Mono<CommonResponse<Iterable<NailSetEmbedDTO<NailImageUrlDTO>>>> getUserNailSets(
        UserAuthByTokenPayload payload,
        Pageable page
) {
    return userService.getUserNailSet(payload.getUserId(), page)
            .map(tipEmbedDto -> tipEmbedDto.transform((tip) -> new NailImageUrlDTO(tip.getImageUrl())))
            .collectList()
            .map(CommonResponse::success);
}

/** 사용자의 네일 세트 생성 */
@PostMapping("/me/nail-sets")
public Mono<CommonResponse<NailSetEmbedDTO<NailImageUrlDTO>>> createUserNailSet(
        UserAuthByTokenPayload payload,
        @RequestBody CreateNailSetDTO requestBody
) {
    return userService.createUserNailSet(payload.getUserId(), requestBody.toList())
            .map(tipEmbedDto -> tipEmbedDto.transform((tip) -> new NailImageUrlDTO(tip.getImageUrl())))
            .map(CommonResponse::success);
}
