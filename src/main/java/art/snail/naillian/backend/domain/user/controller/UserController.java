package art.snail.naillian.backend.domain.user.controller;


import art.snail.naillian.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public Mono<ResponseEntity<Map<String, Object>>> getUserInfo(@RequestHeader("Authorization") String accessToken) {
        return userService.getUserInfo(accessToken);
    }

    /** 닉네임 변경 (온보딩 여부 반영) */
    @PatchMapping("/me/nickname")
    public Mono<ResponseEntity<Map<String ,Object>>> updateNickname(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, String> requestBody
    ) {
        return userService.updateNickname(accessToken, requestBody.get("nickname"))
                .map(ResponseEntity::ok);
    }
}
