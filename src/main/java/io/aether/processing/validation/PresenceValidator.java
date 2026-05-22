package io.aether.processing.validation;

import io.aether.domain.Quality;
import io.aether.domain.QualityFlag;
import io.aether.domain.QualityStatus;
import io.aether.domain.SensorReading;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class PresenceValidator implements ReadingValidator {

    @Override
    public SensorReading validate(SensorReading reading) {
        if (reading.value() != null) return reading;
        return withFlag(reading, QualityFlag.MISSING_VALUE);
    }

    private SensorReading withFlag(SensorReading r, QualityFlag flag) {
        var flags = new HashSet<>(r.quality().flags());
        flags.add(flag);
        var status = r.quality().status() == QualityStatus.OK ? QualityStatus.SUSPECT : r.quality().status();
        return new SensorReading(r.sensorId(), r.schemaVersion(), r.location(), r.metric(), r.unit(),
                r.value(), r.observedAt(), r.ingestedAt(), r.source(), new Quality(status, flags));
    }
}
