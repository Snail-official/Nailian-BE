package art.snail.naillian.backend.domain.user.service;

import art.snail.naillian.backend.domain.auth.service.AuthenticationService;
import art.snail.naillian.backend.domain.user.dto.UserResponseDTO;
import art.snail.naillian.backend.domain.user.entity.SocialLogin;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.entity.UserType;
import art.snail.naillian.backend.domain.user.repository.SocialLoginRepository;
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
    private final SocialLoginRepository socialLoginRepository;
    
    /** 카카오 ID 기반 기존 회원 조회 또는 자동 회원가입 */
    public Mono<User> findOrCreate(String platformUserId, String nickname){
        return socialLoginRepository.findByPlatformUserId(platformUserId)
                .flatMap(socialLogin -> userRepository.findById(socialLogin.getUserId()))
                .switchIfEmpty(createNewUser(platformUserId, nickname));
    }
    
    /** 신규 회원가입 */
    private Mono<User> createNewUser(String platformUserId, String nickname) {
        return userRepository.save(
                User.builder()
                        .nickname(nickname)
                        .userType(UserType.CUSTOMER)
                        .registeredIp("UNKNOWN")
                        .createdAt(LocalDateTime.now())
                        .build()
        )
                .flatMap(user -> socialLoginRepository.save(
                        SocialLogin.builder()
                                .userId(user.getId())
                                .platform("KAKAO")
                                .platformUserId(platformUserId)
                                .build()
                ).thenReturn(user));
    }

    /** 참고하려고 임시로 만들어둔 로직입니다. */
    /** 사용자 저장 */
    public Mono<User> save(User user) {
        return userRepository.save(user);
    }

    /** 사용자 삭제(논리 삭제)*/
    public Mono<Void> deleteUser(Integer userId) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setDeletedAt(LocalDateTime.now()); // 논리 삭제 (deletedAt 설정)
                    return userRepository.save(user);
                })
                .then();
    }

    /** 기존 회원 정보 불러오기 */
    public Mono<UserResponseDTO> getUserInfo(String token) {
        return authenticationService.extractUserIdFromToken(token)
                .flatMap(userRepository::findById)
                .map(UserResponseDTO::from)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다.")));
    }
}
