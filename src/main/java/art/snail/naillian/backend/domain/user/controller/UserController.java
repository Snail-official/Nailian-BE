package art.snail.naillian.backend.domain.user.controller;


import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.user.dto.UserResponseDTO;
import art.snail.naillian.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    /** 사용자 조회 */
    @GetMapping("/me")
    public Mono<CommonResponse<UserResponseDTO>> getUserInfo() {
        return userService.getUserInfo();
    }

    /** 닉네임 변경 (온보딩 여부 반영) */
    @PatchMapping("/me/nickname")
    public Mono<CommonResponse<UserResponseDTO>> updateNickname(@RequestBody Map<String, String> requestBody){
        return userService.updateNickname(requestBody.get("nickname"));
    }
}
