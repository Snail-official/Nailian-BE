package art.snail.naillian.backend.domain.user.controller;


import art.snail.naillian.backend.domain.auth.service.AuthenticationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final AuthenticationService authenticationService;

    /** 사용자 조회 */
    @GetMapping("/me")
    public Mono<ResponseEntity<Map<String, Object>>> getUserInfo(@RequestHeader("Authorization") String accessToken) {
        return authenticationService.getUserFromToken(accessToken)
                .map(user -> ResponseEntity.ok(Map.of(
                        "code", 200,
                        "message", "사용자 정보 조회 성공",
                        "data", Map.of(
                                "userId", user.getId(),
                                "nickname", user.getNickname(),
                                "profileImageUrl", user.getProfileImageUrl(),
                                "createdAt", user.getCreatedAt()
                        )
                )));
    }
}
