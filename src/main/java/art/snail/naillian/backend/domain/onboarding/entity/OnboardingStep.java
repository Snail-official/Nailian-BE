package art.snail.naillian.backend.domain.onboarding.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

import java.io.IOException;

@Getter
@JsonSerialize(using = OnboardingStep.OnboardingStepSerializer.class)
public enum OnboardingStep {
    NICKNAME(0, 1),
    PREFERENCES(1, 2),
    ;

    private final int bitmask;
    private final int requiredVersion;

    public static final int ALL_STEP_BITS;

    static {
        int bit = 0;
        for (OnboardingStep step : OnboardingStep.values()) {
            bit |= step.bitmask;
        }
        ALL_STEP_BITS = bit;
    }

    OnboardingStep(int order, int requiredVersion) {
        this.bitmask = 1 << order;
        this.requiredVersion = requiredVersion;
    }

    public String getSerializeName() {
        return "Onboarding" +
                PropertyNamingStrategies.UpperCamelCaseStrategy.INSTANCE.translate(name().toLowerCase());
    }

    public static class OnboardingStepSerializer extends JsonSerializer<OnboardingStep> {
        @Override
        public void serialize(OnboardingStep step, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeObject(step.getSerializeName());
        }
    }
}
