package art.snail.naillian.backend.domain.user.service;

import art.snail.naillian.backend.domain.onboarding.entity.OnboardingStep;
import art.snail.naillian.backend.domain.onboarding.service.OnboardingService;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final OnboardingService onboardingService;

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[ㄱ-ㅎ가-힣a-zA-Z0-9]{2,8}$");

    /**
     * 기존 회원 정보 불러오기
     */
    public Mono<User> getUserById(int id) {
        return userRepository.findById(id);
    }

    public Mono<User> getUserByNickname(String nickname) {
        return userRepository.findByNickname(nickname);
    }

    /**
     * 닉네임 변경 (온보딩 여부에 따라 처리)
     */
    public Mono<Tuple2<User, String>> updateNickname(int userId, String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "닉네임을 입력해주세요."));
        }
        if (!NICKNAME_PATTERN.matcher(newNickname).matches()) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "닉네임은 한글, 영문, 숫자만 사용 가능하며 2~8자로 입력해야 합니다."));
        }

        return Mono.zip(
                        this.getUserById(userId),
                        this.getUserByNickname(newNickname)
                                .map(Optional::of)
                                .defaultIfEmpty(Optional.empty())
                )
                .flatMap(tuple -> {
                    User user = tuple.getT1();
                    Optional<User> existingUser = tuple.getT2();

                    if (existingUser.isPresent() && existingUser.get().getId() != userId)
                        return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다."));

                    return Mono.just(user);
                })
                .flatMap(user -> {
                    user.setNickname(newNickname);
                    String message = OnboardingService.markOnboardingComplete(user, OnboardingStep.NICKNAME)
                            ? "닉네임이 성공적으로 저장되었습니다. (온보딩 완료)"
                            : "닉네임이 변경되었습니다.";

                    return userRepository.save(user)
                            .zipWith(Mono.just(message));
                });
    }
}
