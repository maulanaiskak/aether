package io.aether.processing.validation;

import io.aether.domain.Quality;
import io.aether.domain.QualityFlag;
import io.aether.domain.QualityStatus;
import io.aether.domain.SensorReading;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;

@Component
public class StalenessValidator implements ReadingValidator {

    private final Duration maxAge;

    public StalenessValidator() {
        this.maxAge = Duration.ofHours(3);
    }

    StalenessValidator(Duration maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public SensorReading validate(SensorReading reading) {
        var age = Duration.between(reading.observedAt(), Instant.now());
        if (age.compareTo(maxAge) > 0) {
            return withFlag(reading, QualityFlag.STALE);
        }
        return reading;
    }

    private SensorReading withFlag(SensorReading r, QualityFlag flag) {
        var flags = new HashSet<>(r.quality().flags());
        flags.add(flag);
        var status = r.quality().status() == QualityStatus.OK ? QualityStatus.SUSPECT : r.quality().status();
        return new SensorReading(r.sensorId(), r.schemaVersion(), r.location(), r.metric(), r.unit(),
                r.value(), r.observedAt(), r.ingestedAt(), r.source(), new Quality(status, flags));
    }
}
