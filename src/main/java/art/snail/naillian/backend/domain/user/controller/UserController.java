package art.snail.naillian.backend.domain.user.controller;


import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.auth.jwt.UserAuthByTokenPayload;
import art.snail.naillian.backend.domain.auth.utils.AuthUtil;
import art.snail.naillian.backend.domain.user.dto.UserResponseDTO;
import art.snail.naillian.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final Mono<SecurityContext> ctx = ReactiveSecurityContextHolder.getContext();

    /** 사용자 조회 */
    @GetMapping("/me")
    public Mono<CommonResponse<UserResponseDTO>> getUserInfo() {
        return AuthUtil.getContextUser(ctx)
                .map(UserAuthByTokenPayload::getUserId)
                .flatMap(userService::getUserById)
                .map(UserResponseDTO::from)
                .map(dto -> CommonResponse.success(dto, "사용자 정보 조회 성공"));
    }

    /** 닉네임 변경 (온보딩 여부 반영) */
    @PatchMapping("/me/nickname")
    public Mono<ResponseEntity<Map<String ,Object>>> updateNickname(
            @RequestBody Map<String, String> requestBody
    ) {
        return AuthUtil.getContextUser(ctx)
                .map(UserAuthByTokenPayload::getUserId)
                .flatMap(userId -> userService.updateNickname(userId, requestBody.get("nickname")))
                .map(ResponseEntity::ok);
    }
}
