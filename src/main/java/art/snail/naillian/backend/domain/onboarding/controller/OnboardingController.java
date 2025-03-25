package art.snail.naillian.backend.domain.onboarding.controller;

import art.snail.naillian.backend.common.CommonResponse;
import art.snail.naillian.backend.domain.auth.jwt.UserAuthByTokenPayload;
import art.snail.naillian.backend.domain.onboarding.dto.OnboardingStepDTO;
import art.snail.naillian.backend.domain.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingService onboardingService;

    @GetMapping("/onboarding-status")
    public Mono<CommonResponse<OnboardingStepDTO>> getOnboardingStatus(
            @RequestParam(name = "maxSupportedVersion", required = false, defaultValue = "1") int maxSupportedVersion
    ) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (UserAuthByTokenPayload) securityContext.getAuthentication())
                .map(UserAuthByTokenPayload::getUserId)
                .flatMap(userId -> onboardingService.getNextOnboardingStep(userId, maxSupportedVersion))
                .map(OnboardingStepDTO::new)
                .map(CommonResponse::success);
    }
}
