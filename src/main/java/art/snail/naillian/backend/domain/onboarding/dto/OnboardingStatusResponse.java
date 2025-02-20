package art.snail.naillian.backend.domain.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OnboardingStatusResponse {
    private final int code;
    private final String message;
    private final Data data;

    public static OnboardingStatusResponse success(String nextStep){
        return new OnboardingStatusResponse(200, "온보딩 상태 조회 성공", new Data(nextStep));
    }

    @Getter
    @AllArgsConstructor
    public static class Data {
        private final String nextOnboardingStep;
    }
}
