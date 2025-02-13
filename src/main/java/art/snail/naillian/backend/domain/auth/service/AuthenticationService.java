package art.snail.naillian.backend.domain.auth.service;

import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.user.entity.SocialLogin;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.entity.UserType;
import art.snail.naillian.backend.domain.user.repository.SocialLoginRepository;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import art.snail.naillian.backend.errors.ReportableError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/** 회원가입 + JWT 토큰 발급 담당 */

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final TokenService tokenService;
    private final KakaoAuthService kakaoAuthService;

    /** 새로운 사용자 생성 */
    // TODO: 스토어 서비스로 분리
    private Mono<User> createNewUser(Map<String, String> userInfo) {
        return userRepository.save(User.builder()
                        .nickname(userInfo.get("nickname"))
                        .userType(UserType.CUSTOMER)
                        .registeredIp("UNKNOWN")
                        .build())
                .flatMap(user -> socialLoginRepository.save(
                        SocialLogin.builder()
                                .userId(user.getId())
                                .platform("KAKAO")
                                .platformUserId(userInfo.get("platformUserId"))
                                .build()
                ).thenReturn(user));
    }

    /** 카카오 사용자 정보 기반으로 회원가입 진행 */
    public Mono<Map<String, String>> signUpWithKakao(String authorizationCode) {
        return kakaoAuthService.getAccessToken(authorizationCode)
                .flatMap(kakaoAuthService::getUserInfo)
                .flatMap(userInfo -> {
                    String platformUserId = userInfo.get("platformUserId");
                    return kakaoAuthService.findUserByKakaoId(platformUserId)
                            .switchIfEmpty(createNewUser(userInfo));
                })
                .flatMap(user -> {
                    String accessToken = jwtProvider.generateAccessToken(user.getId());
                    String refreshToken = jwtProvider.generateRefreshToken(user.getId());

                    return Mono.just(Map.of(
                            "nickname", user.getNickname(),
                            "accessToken", accessToken,
                            "refreshToken", refreshToken
                    ));
                });
    }


    /** Access Token 검증 및 사용자 ID 추출 */
    public Mono<Integer> extractUserIdFromToken(String authorizationHeader) {
        return Mono.justOrEmpty(authorizationHeader)
                .map(this::parseToken)
                .flatMap(jwtProvider::getUserIdFromToken)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED ,"유효하지 않은 인증 정보입니다.")));
    }

    /** AccessToken을 통해 사용자 조회 */
    public Mono<User> getUserFromToken(String authorizationHeader) {
        return extractUserIdFromToken(authorizationHeader)
                .flatMap(userRepository::findById)
                .switchIfEmpty(Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.")));
    }

    /** Beaer 토큰에서 실제 토큰 값 추출 */
    private String parseToken(String authorizationHeader) {
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ReportableError(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다.", 401);
        }
        return authorizationHeader.substring(7);
    }

    /** refreshToken을 사용해 새로운 accessToken 발급 */
    public Mono<String> reIssueAccessToken(String refreshToken) {
        return tokenService.getUserIdFromRefreshToken(refreshToken)
                .flatMap(userId -> {
                    if (userId == null) {
                        return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다."));
                    }
                    return Mono.just(jwtProvider.generateAccessToken(userId));
                });
    }

    /** 로그아웃 */
    public Mono<Void> logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Mono.error(new ReportableError(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        }
        tokenService.invalidateAccessToken(accessToken);
        return Mono.empty();
    }
}
