package art.snail.naillian.backend.domain.user.service;

import art.snail.naillian.backend.domain.auth.service.AuthenticationService;
import art.snail.naillian.backend.domain.user.dto.UserResponseDTO;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import art.snail.naillian.backend.errors.ReportableError;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public Mono<UserResponseDTO> getUserInfo(String token) {
        return authenticationService.extractUserIdFromToken(token)
                .flatMap(userRepository::findById)
                .map(UserResponseDTO::from)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다.", 404)));
    }

    /** 1. 사용자 저장 */
    public Mono<User> save(User user) {
        return userRepository.save(user);
    }

    /** 2. 사용자 삭제(논리 삭제)*/
    public Mono<Void> deleteUser(Integer userId) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setDeletedAt(LocalDateTime.now()); // 논리 삭제 (deletedAt 설정)
                    return userRepository.save(user);
                })
                .then();
    }
}
