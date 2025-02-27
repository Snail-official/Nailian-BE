package art.snail.naillian.backend.domain.onboarding.controller;

import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.auth.jwt.UserAuthByTokenPayload;
import art.snail.naillian.backend.domain.onboarding.dto.OnboardingStatusResponse;
import art.snail.naillian.backend.domain.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/onboarding-status")
    public Mono<ResponseEntity<OnboardingStatusResponse>> getOnboardingStatus(
            @RequestParam(name = "maxSupportedVersion", required = false, defaultValue = "1") int maxSupportedVersion
    ) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (UserAuthByTokenPayload) securityContext.getAuthentication())
                .map(UserAuthByTokenPayload::getUserId)
                .flatMap(userId -> onboardingService.getNextOnboardingStep(userId, maxSupportedVersion))
                .map(ResponseEntity::ok);
    }
}
