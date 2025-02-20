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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]{2,8}$");

    private static  final int ONBOARDING_NICKNAME_FLAG = 0x01;

    /** 기존 회원 정보 불러오기 */
    public Mono<ResponseEntity<Map<String, Object>>> getUserInfo(String token) {
        return authenticationService.extractUserIdFromToken(token)
                .flatMap(userRepository::findById)
                .map(UserResponseDTO::from)
                .map(userResponse -> {
                    String profileImage = userResponse.getProfileImage() != null ? userResponse.getProfileImage() : "";
                    int onboardingProgress = userResponse.getOnboardingProgress() != null ? userResponse.getOnboardingProgress() : 0;

                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("id", userResponse.getId());
                    data.put("nickname", userResponse.getNickname());
                    data.put("profileImage", profileImage);
                    data.put("onboardingProgress", onboardingProgress);

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("code", 200);
                    response.put("message", "사용자 정보 조회 성공");
                    response.put("data", data);

                    return ResponseEntity.ok(response);
                })
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")));
    }



    /**
     * 닉네임 변경 (온보딩 여부에 따라 처리)
     */
    public Mono<Map<String, Object>> updateNickname(String token, String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "닉네임을 입력해주세요."));
        }
        if (!NICKNAME_PATTERN.matcher(newNickname).matches()) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "닉네임은 한글, 영문, 숫자만 사용 가능하며 2~8자로 입력해야 합니다."));
        }

        return authenticationService.extractUserIdFromToken(token)
                .flatMap(userId -> userRepository.findById(userId)
                        .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다.")))
                        .flatMap(user -> {
                            boolean isOnboarding = (user.getNickname() == null || user.getNickname().isBlank());

                            user.setNickname(newNickname);
                            if (isOnboarding && (user.getOnboardingStepsBitmask() & ONBOARDING_NICKNAME_FLAG) == 0) {
                                user.setOnboardingStepsBitmask(user.getOnboardingStepsBitmask() | ONBOARDING_NICKNAME_FLAG);
                            }

                            return userRepository.save(user)
                                    .map(updatedUser -> createResponse(updatedUser, isOnboarding));
                        })
                );
    }

    /**
     * 변경 후 공통 응답
     */
    private Map<String, Object> createResponse(User user, boolean isOnboarding){
        return Map.of(
                "code", 200,
                "message", isOnboarding ? "닉네임이 성공적으로 저장되었습니다. (온보딩 완료)" : "닉네임이 변경 되었습니다.",
                "data", Map.of(
                        "id", user.getId(),
                        "nickname", user.getNickname(),
                        "profileImageUrl", user.getProfileImageUrl(),
                        "onboardingProgress", user.getOnboardingStepsBitmask()
                )
        );
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
}
