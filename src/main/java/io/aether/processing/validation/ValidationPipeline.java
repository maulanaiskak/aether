package io.aether.processing.validation;

import io.aether.domain.SensorReading;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidationPipeline {

    private final List<ReadingValidator> validators;

    public ValidationPipeline(
            PresenceValidator presenceValidator,
            RangeValidator rangeValidator,
            StalenessValidator stalenessValidator) {
        this.validators = List.of(presenceValidator, rangeValidator, stalenessValidator);
    }

    public SensorReading validate(SensorReading reading) {
        return validators.stream()
                .reduce(reading,
                        (r, validator) -> validator.validate(r),
                        (a, b) -> b);
    }
}
