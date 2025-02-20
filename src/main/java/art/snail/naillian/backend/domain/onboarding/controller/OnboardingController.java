package art.snail.naillian.backend.domain.onboarding.controller;

import art.snail.naillian.backend.domain.auth.jwt.JwtProvider;
import art.snail.naillian.backend.domain.onboarding.dto.OnboardingStatusResponse;
import art.snail.naillian.backend.domain.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/onboarding-status")
    public Mono<OnboardingStatusResponse> getOnboardingStatus(@RequestHeader("Authorization") String authorization) {
        return onboardingService.getNextOnboardingStep(authorization);
    }
}
