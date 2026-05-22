package io.aether.api.dto;

import io.aether.domain.AnomalyEvent;
import io.aether.domain.SensorReading;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public SensorReadingDto toDto(SensorReading reading) {
        return new SensorReadingDto(
                reading.sensorId().toString(),
                reading.location(),
                reading.metric().name(),
                reading.unit(),
                reading.value(),
                null,
                reading.observedAt(),
                reading.quality().status().name()
        );
    }

    public AnomalyEventDto toDto(AnomalyEvent event) {
        return new AnomalyEventDto(
                event.sensorId().toString(),
                event.location(),
                event.metric().name(),
                event.observedAt(),
                event.value(),
                event.method(),
                event.score()
        );
    }
}
