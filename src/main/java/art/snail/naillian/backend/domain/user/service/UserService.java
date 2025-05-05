package art.snail.naillian.backend.domain.user.service;

import art.snail.naillian.backend.common.PageDTO;
import art.snail.naillian.backend.domain.nail.dto.NailImageUrlDTO;
import art.snail.naillian.backend.domain.nail.dto.NailSetEmbedDTO;
import art.snail.naillian.backend.domain.nail.entity.NailFolderSet;
import art.snail.naillian.backend.domain.nail.entity.NailTip;
import art.snail.naillian.backend.domain.nail.repository.NailFolderSetRepository;
import art.snail.naillian.backend.domain.nail.service.NailService;
import art.snail.naillian.backend.domain.onboarding.entity.OnboardingStep;
import art.snail.naillian.backend.domain.onboarding.service.OnboardingService;
import art.snail.naillian.backend.domain.user.dto.EventSubmissionDTO;
import art.snail.naillian.backend.domain.user.entity.EventSubmission;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.repository.EventSubmissionRepository;
import art.snail.naillian.backend.domain.user.repository.SocialLoginRepository;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EventSubmissionRepository eventRepository;
    private final NailFolderSetRepository folderSetRepository;
    private static final Pattern EMAIL_OR_PHONE = Pattern.compile("(^[^@]+@[^@.]+\\.[^@.\\n]+$)|(^0[15-9][0-9]{1,2}-[0-9]{3,4}-[0-9]{3,5}$)");
    private final NailService nailService;
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[ㄱ-ㅎ가-힣a-zA-Z0-9]{2,8}$");

    private static final int EVENT_FOLDER_ID = 4;
    private final SocialLoginRepository socialLoginRepository;

    /**
     * 기존 회원 정보 불러오기
     */
    public Mono<User> getUserById(int id) {

        return userRepository.findById(id)
                .filter(user -> user.getDeletedAt() == null)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")));
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

    /**
     * 회원 탈퇴(논리적 삭제)
     */
    public Mono<Void> deleteUser(int userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.GONE, "회원 정보가 존재하지 않습니다. 다시 로그인해주세요.")))
                .flatMap(user -> {
                    if (user.getDeletedAt() != null) {
                        return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "이미 탈퇴한 사용자입니다."));
                    }
                    LocalDateTime now = LocalDateTime.now();
                    user.setDeletedAt(now);
                    return userRepository.save(user)
                            .flatMap(savedUser ->
                                    socialLoginRepository.findAllByUserId(userId)
                                            .flatMap(sl -> {
                                                sl.setDeletedAt(now);
                                                long epoch = now.atZone(ZoneId.systemDefault()).toEpochSecond();
                                                sl.setPlatformUserId(sl.getPlatformUserId() + "_deleted_at_" + epoch);
                                                return socialLoginRepository.save(sl);
                                            })
                                            .then()
                            );
                })
                .then();
    }

    /**
     * 사용자의 네일 세트 조회
     */
    public Mono<Page<NailSetEmbedDTO<NailImageUrlDTO>>> getUserNailSet(Integer userId, Pageable page) {
        return nailService.getUserNailSetCount(userId)
                .flatMap(count -> (count == 0)
                        ? Mono.just(new PageDTO<>(List.of(), page, 0))
                        : nailService.getUserNailSets(userId, page)
                        .flatMap(nailSet -> nailService.getNailSetWithNailTip(nailSet.getId()))
                        .map(dto -> dto.transform(NailImageUrlDTO::from))
                        .collectList()
                        .map(list -> new PageDTO<>(list, page, count))
                );
    }

    /**
     * 사용자의 네일 세트 생성
     */
    public Mono<NailSetEmbedDTO<NailTip>> createUserNailSet(Integer userId, List<Integer> tipIds) {
        return nailService.createUserNailSet(userId, tipIds)
                .flatMap(nailSet -> nailService.getNailSetWithNailTip(nailSet.getId()));
    }

    /**
     * 사용자의 네일 세트 보관
     *
     * @return 복제된 네일 세트의 정보
     */
    public Mono<NailSetEmbedDTO<NailTip>> scrapNailSetForUser(Integer userId, Integer nailSetId) {
        if (nailSetId == null)
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "네일 세트 아이디를 지정해주세요."));

        return getUserById(userId)
                .then(nailService.cloneNailSetForUser(userId, nailSetId))
                .flatMap(nailSet -> nailService.getNailSetWithNailTip(nailSet.getId()));
    }

    /**
     * 사용자 네일 세트 보관 해제
     */
    public Mono<Void> unScrapNailSetForUser(Integer userId, Integer nailSetId) {
        if (nailSetId == null)
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "네일 세트 아이디를 지정해주세요."));

        return getUserById(userId)
                .then(nailService.deleteNailSetEnsureUser(userId, nailSetId))
                .then();
    }

    /**
     * 아트 이벤트 응모
     */
    @Transactional
    public Mono<Void> submitEvent(int userId, EventSubmissionDTO event) {
        if (!EMAIL_OR_PHONE.matcher(event.getUserInfo()).matches()) {
            return Mono.error(new ReportableError(
                    HttpStatus.BAD_REQUEST,
                    "입력하신 이메일 혹은 전화번호가 올바르지 않습니다."
            ));
        }

        return nailService.getNailSet(event.getNailSetId())
                .filter(set -> Objects.equals(set.getUploadedBy(), userId))
                .switchIfEmpty(Mono.error(new ReportableError(
                        HttpStatus.NOT_FOUND,
                        "네일 세트를 찾을 수 없습니다."
                )))
                .zipWith(
                        eventRepository.existsByUserId(userId)
                                .filter(exists -> !exists)
                                .switchIfEmpty(Mono.error(new ReportableError(
                                        HttpStatus.BAD_REQUEST,
                                        "이미 응모하셨습니다."
                                )))
                )
                .map(__ -> {
                    boolean isEmail = event.getUserInfo().contains("@");
                    return EventSubmission.builder()
                            .userId(userId)
                            .nailSetId(event.getNailSetId())
                            .email(isEmail ? event.getUserInfo() : null)
                            .phoneNumber(isEmail ? null : event.getUserInfo())
                            .createdAt(LocalDateTime.now(Clock.systemUTC()))
                            .build();
                })
                .flatMap(eventRepository::save)
                .flatMap(saved ->
                        folderSetRepository.save(new NailFolderSet(
                                null,
                                EVENT_FOLDER_ID,
                                saved.getNailSetId(),
                                LocalDateTime.now()
                        ))
                )
                .then();
    }

    public Mono<Boolean> hasEnrolledEvent(int userId) {
        return eventRepository.existsByUserId(userId);
    }
}
