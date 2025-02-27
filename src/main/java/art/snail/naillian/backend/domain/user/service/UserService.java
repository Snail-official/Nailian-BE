package art.snail.naillian.backend.domain.user.service;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.auth.jwt.UserAuthByTokenPayload;
import art.snail.naillian.backend.domain.auth.service.AuthenticationService;
import art.snail.naillian.backend.domain.user.dto.UserResponseDTO;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[ㄱ-ㅎ가-힣a-zA-Z0-9]{2,8}$");

    private static final int ONBOARDING_NICKNAME_FLAG = 0x01;

    /**
     * 기존 회원 정보 불러오기
     */
    public Mono<CommonResponse<UserResponseDTO>> getUserInfo() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (UserAuthByTokenPayload) ctx.getAuthentication())
                .flatMap(userAuth -> userRepository.findById(userAuth.getUserId()))
                .map(UserResponseDTO::from)
                .map(userResponse -> CommonResponse.success(userResponse, "사용자 정보 조회 성공"))
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")));
    }

    public Mono<User> getUserById(int id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")));
    }


    /**
     * 닉네임 변경 (온보딩 여부에 따라 처리)
     */
    public Mono<CommonResponse<UserResponseDTO>> updateNickname(String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "닉네임을 입력해주세요."));
        }
        if (!NICKNAME_PATTERN.matcher(newNickname).matches()) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "닉네임은 한글, 영문, 숫자만 사용 가능하며 2~8자로 입력해야 합니다."));
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (UserAuthByTokenPayload) ctx.getAuthentication())
                .flatMap(userAuth -> userRepository.findById(userAuth.getUserId()))
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다.")))
                .flatMap(user -> {
                    boolean isOnboarding = (user.getNickname() == null || user.getNickname().isBlank());

                    user.setNickname(newNickname);
                    if (isOnboarding && (user.getOnboardingStepsBitmask() & ONBOARDING_NICKNAME_FLAG) == 0) {
                        user.setOnboardingStepsBitmask(user.getOnboardingStepsBitmask() | ONBOARDING_NICKNAME_FLAG);
                }
                return userRepository.save(user)
                        .map(updatedUser -> {
                            String message = isOnboarding
                                    ? "닉네임이 성공적으로 저장되었습니다. (온보딩 완료)"
                                    : "닉네임이 변경되었습니다.";
                            return CommonResponse.success(UserResponseDTO.from(updatedUser), message);
                        });
                });
    }
}
