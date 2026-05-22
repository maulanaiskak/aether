package io.aether.domain.event;

import io.aether.domain.SensorReading;
import org.springframework.context.ApplicationEvent;

public class ReadingValidatedEvent extends ApplicationEvent {

    private final SensorReading reading;

    public ReadingValidatedEvent(Object source, SensorReading reading) {
        super(source);
        this.reading = reading;
    }

    public SensorReading reading() {
        return reading;
    }
}
