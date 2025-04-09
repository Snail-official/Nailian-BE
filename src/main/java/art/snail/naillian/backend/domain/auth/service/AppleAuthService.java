package art.snail.naillian.backend.domain.auth.service;

import art.snail.naillian.backend.domain.auth.dto.AppleAuthRequest;
import art.snail.naillian.backend.domain.auth.dto.UserTokenPairDTO;
import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.user.entity.SocialLogin;
import art.snail.naillian.backend.domain.user.entity.SocialPlatform;
import art.snail.naillian.backend.domain.user.entity.User;
import art.snail.naillian.backend.domain.user.entity.UserType;
import art.snail.naillian.backend.domain.user.repository.SocialLoginRepository;
import art.snail.naillian.backend.domain.user.repository.UserRepository;
import art.snail.naillian.backend.errors.ReportableError;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import reactor.util.function.Tuple2;

@Service
@RequiredArgsConstructor
public class AppleAuthService {

    @Value("${apple.team-id}")
    private String APPLE_TEAM_ID;

    @Value("${apple.login-key}")
    private String APPLE_LOGIN_KEY;

    @Value("${apple.client-id}")
    private String APPLE_CLIENT_ID;

    @Value("${apple.redirect-url}")
    private String APPLE_REDIRECT_URL;

    @Value("${apple.key-path}")
    private String APPLE_KEY_PATH;

    private final static String APPLE_AUTH_URL = "https://appleid.apple.com";

    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 1000 * 60 * 5;

    private static final Logger log = LoggerFactory.getLogger(AppleAuthService.class);


    // Client_Secret 리팩터링을 위한 상수형 변수들
    private static final long CLIENT_SECRET_EXPIRATION_MILLIS = 1000L * 60 * 60 * 24 * 180; // 180일
    private static final long CLIENT_SECRET_REFRESH_THRESHOLD_MILLIS = 1000L * 60 * 60 * 24; // 만료 1일 전이면 갱신

    private volatile String cachedClientSecret = null;
    private volatile long cachedClientSecretExp = 0;

    private final Object lock = new Object();

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final TokenService tokenService;
    private final SocialLoginRepository socialLoginRepository;

    private final WebClient webClient = WebClient.create();

    private PrivateKey cachedPrivateKey;

    @PostConstruct
    private void init(){
        try{
            this.cachedPrivateKey = loadPrivateKey(APPLE_KEY_PATH);
        } catch (Exception e) {
            throw new IllegalArgumentException("Apple private key 초기화 실패", e);
        }
    }

    /**
     * 애플 로그인 처리:
     * - 클라이언트로부터 전달받은 identityToken, authorizationCode, user 정보를 사용
     * - JWT를 통해 Access/Refresh Token 발급 ㅎ TokenService를 통해 Redis에 저장
     */
    public Mono<UserTokenPairDTO> handleAppleLogin(AppleAuthRequest request) {
        if (request.getAuthorizationCode() == null) {
            return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "authorizationCode가 누락되었습니다."));
        }

        return exchangeAuthorizationCodeWithApple(request.getAuthorizationCode())
                .flatMap(tokenRes -> verifyIdentityToken(tokenRes.idToken)) // Apple에서 받은 id_token 검증
                .flatMap(claims -> {
                    String sub = claims.getSubject(); // Apple 고유 식별자
                    return findOrCreateUser(request, sub);
                })
                .map(user -> {
                    Date now = new Date();
                    String accessToken = jwtProvider.generateAccessToken(user.getId(), now);
                    String refreshToken = jwtProvider.generateRefreshToken(user.getId(), now);
                    return Tuples.of(new UserTokenPairDTO(accessToken, refreshToken), user.getId());
                })
                .doOnNext(tuple -> {
                    UserTokenPairDTO dto = tuple.getT1();
                    Integer userId = tuple.getT2();
                    tokenService.storeTokenPair(dto.getAccessToken(), dto.getRefreshToken(), userId);
                })
                .map(Tuple2::getT1);
    }





    private Mono<AppleTokenResponse> exchangeAuthorizationCodeWithApple(String authorizationCode) {
        return generateClientSecret().flatMap(clientSecret -> {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", APPLE_CLIENT_ID);
            form.add("client_secret", clientSecret);
            form.add("code", authorizationCode);
            form.add("grant_type", "authorization_code");
            form.add("redirect_uri", APPLE_REDIRECT_URL);

            return webClient.post()
                    .uri("https://appleid.apple.com/auth/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(form)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response ->
                            response.bodyToMono(String.class)
                                    .doOnNext(body -> log.warn("Apple 인증 4xx 오류 응답: {}", body))
                                    .then(Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "Apple 인증 요청에 문제가 발생했습니다.")))
                    )
                    .onStatus(status -> status.is5xxServerError(), response ->
                            response.bodyToMono(String.class)
                                    .doOnNext(body -> log.error("Apple 인증 5xx 서버 오류 응답: {}", body))
                                    .then(Mono.error(new ReportableError(HttpStatus.INTERNAL_SERVER_ERROR, "Apple 서버 오류가 발생했습니다. 나중에 다시 시도해주세요.")))
                    )
                    .bodyToMono(AppleTokenResponse.class);
        });
    }




    private Mono<String> generateClientSecret() {
        return Mono.fromCallable(() -> {
            long now = System.currentTimeMillis();

            if (cachedClientSecret != null && (cachedClientSecretExp - now) > CLIENT_SECRET_REFRESH_THRESHOLD_MILLIS) {
                return cachedClientSecret;
            }

            synchronized (lock) {
                now = System.currentTimeMillis();
                if (cachedClientSecret != null && (cachedClientSecretExp - now) > CLIENT_SECRET_REFRESH_THRESHOLD_MILLIS) {
                    return cachedClientSecret;
                }

                Date issuedAt = new Date(now);
                Date expiration = new Date(now + CLIENT_SECRET_EXPIRATION_MILLIS);

                String newClientSecret = Jwts.builder()
                        .setHeaderParam("kid", APPLE_LOGIN_KEY)
                        .setIssuer(APPLE_TEAM_ID)
                        .setIssuedAt(issuedAt)
                        .setExpiration(expiration)
                        .setAudience(APPLE_AUTH_URL)
                        .setSubject(APPLE_CLIENT_ID)
                        .signWith(cachedPrivateKey, io.jsonwebtoken.SignatureAlgorithm.ES256)
                        .compact();

                cachedClientSecret = newClientSecret;
                cachedClientSecretExp = expiration.getTime();

                return newClientSecret;
            }
        });
    }


    /**
     * Private Key 로딩 메서드인데 한번 호출 후 캐싱
     * PostConstruct에서 동기 호출
     */
    private PrivateKey loadPrivateKey(String keyPath) throws IOException, NoSuchAlgorithmException,  InvalidKeySpecException{
        String key = Files.readString(Path.of(keyPath))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(spec);
    }




    /**
     * identityToken의 서명을 Apple 공개키로 검증한 후 Claims를 반환.
     * issuer와 audience 검증 실패 시 명시적으로 Mono.error() 호출
     */
    private Mono<Claims> verifyIdentityToken(String identityToken) {
        return extractKidFromToken(identityToken)
                .flatMap(this::getApplePublicKey)
                .flatMap(publicKey ->
                        Mono.fromCallable(() -> {
                                    Claims claims = Jwts.parserBuilder()
                                            .setSigningKey(publicKey)
                                            .build()
                                            .parseClaimsJws(identityToken)
                                            .getBody();
                                    if (!APPLE_AUTH_URL.equals(claims.getIssuer())) {
                                        throw new ReportableError(HttpStatus.BAD_REQUEST, "유효하지 않은 issuer입니다.");
                                    }
                                    if (!APPLE_CLIENT_ID.equals(claims.getAudience())) {
                                        throw new ReportableError(HttpStatus.BAD_REQUEST, "유효하지 않은 audience입니다.");
                                    }
                                    return claims;
                                })
                                .onErrorMap(e -> new ReportableError(HttpStatus.BAD_REQUEST, "유효하지 않은 identityToken입니다."))
                );
    }

    /**
     * identityToken에서 kid 추출
     */
    private Mono<String> extractKidFromToken(String identityToken){
        return Mono.fromCallable(() -> {
            String[] parts = identityToken.split("\\.");
            if (parts.length < 2){
                throw new ReportableError(HttpStatus.BAD_REQUEST, "유효하지 않은 identityToken입니다.");
            }
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header = new ObjectMapper().readTree(headerJson);
            if(header.get("kid") == null){
                throw new ReportableError(HttpStatus.BAD_REQUEST, "토큰 헤더에 kid가 없습니다.");
            }
            return header.get("kid").asText();
        });
    }

    /**
     * Apple 공개키 엔드포인트에서 JWK(Json Web Key) 목록을 조회한 후, 요청한 kid와 일치하는 PublicKey 반환.
     */
    private Mono<PublicKey> getApplePublicKey(String kid) {
        return webClient.get()
                .uri("https://appleid.apple.com/auth/keys")
                .retrieve()
                .bodyToMono(ApplePublicKeys.class)
                .flatMap(appleKeys -> {
                    ApplePublicKey matchingKey = appleKeys.getKeyById(kid);
                    if (matchingKey == null) {
                        return Mono.error(new ReportableError(HttpStatus.BAD_REQUEST, "유효하지 않은 identityToken입니다."));
                    }
                    return Mono.fromCallable(matchingKey::toPublicKey)
                            .onErrorMap(e -> new ReportableError(HttpStatus.BAD_REQUEST, "유효하지 않은 identityToken입니다."));
                });
    }

    /**
     * 기존 소셜 로그인 정보가 있으면 해당 User를, 없으면 새로 회원가입 처리 (탈퇴 사용자 재가입 포함)
     * Apple의 고유 식별자 'sub'를 사용
     */
    private Mono<User> findOrCreateUser(AppleAuthRequest request, String sub) {
        String rawNickname = (request.getUser().getName() != null && !request.getUser().getName().isEmpty())
                ? request.getUser().getName()
                : "apple_" + sub;
        String nickname = rawNickname.length() > 16 ? rawNickname.substring(0, 16) : rawNickname;
        String email = request.getUser().getEmail();

        return socialLoginRepository.findByPlatformUserId(sub)
                .flatMap(socialLogin -> userRepository.findById(socialLogin.getUserId()))
                .flatMap(user -> {
                    if (user.getDeletedAt() != null) {
                        user.setDeletedAt(null);
                        return userRepository.save(user);
                    }
                    return Mono.just(user);
                })
                .switchIfEmpty(createNewUser(sub, nickname, email));
    }




    /**
     * 새 User를 생성하고, Apple 소셜 로그인 레코드를 함께 저장.
     * 개선: 사용자 생성 및 소셜 로그인 등록을 하나의 Mono 체인으로 통합
     */
    private Mono<User> createNewUser(String sub, String nickname, String email) {
        return Mono.fromCallable(() -> User.builder()
                        .nickname(nickname)
                        .userType(UserType.CUSTOMER)
                        .registeredIp("UNKNOWN")
                        .createdAt(LocalDateTime.now())
                        .onboardingStepsBitmask(0)
                        .email(email)
                        .build())
                .flatMap(userRepository::save)
                .flatMap(savedUser -> {
                    SocialLogin sl = SocialLogin.builder()
                            .userId(savedUser.getId())
                            .platform(SocialPlatform.APPLE)
                            .platformUserId(sub)
                            .build();
                    return socialLoginRepository.save(sl)
                            .thenReturn(savedUser);
                });
    }




    static class ApplePublicKeys {
        @JsonProperty("keys")
        private List<ApplePublicKey> keys;

        @Nullable
        public ApplePublicKey getKeyById(String kid) {
            if (keys == null) return null;
            return keys.stream()
                    .filter(key -> kid.equals(key.getKid()))
                    .findFirst()
                    .orElse(null);
        }
    }


    @Data
    public static class ApplePublicKey {
        private String kid;
        private String kty;
        private String alg;
        private String use;
        private String n;
        private String e;

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getKty() {
            return kty;
        }

        public void setKty(String kty) {
            this.kty = kty;
        }

        public String getAlg() {
            return alg;
        }

        public void setAlg(String alg) {
            this.alg = alg;
        }

        public String getUse() {
            return use;
        }

        public void setUse(String use) {
            this.use = use;
        }

        public String getN() {
            return n;
        }

        public void setN(String n) {
            this.n = n;
        }

        public String getE() {
            return e;
        }

        public void setE(String e) {
            this.e = e;
        }

        public PublicKey toPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
            byte[] modulusBytes = Base64.getUrlDecoder().decode(n);
            byte[] exponentBytes = Base64.getUrlDecoder().decode(e);
            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger exponent = new BigInteger(1, exponentBytes);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        }
    }

    static class AppleTokenResponse{
        @JsonProperty("access_token")
        public String accessToken;

        @JsonProperty("refresh_token")
        public String refreshToken;

        @JsonProperty("id_token")
        public String idToken;

        @JsonProperty("token_type")
        public String tokenType;

        @JsonProperty("expires_in")
        public int expiresIn;
    }
}
