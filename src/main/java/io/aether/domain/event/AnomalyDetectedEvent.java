package io.aether.domain.event;

import io.aether.domain.AnomalyEvent;
import org.springframework.context.ApplicationEvent;

public class AnomalyDetectedEvent extends ApplicationEvent {

    private final AnomalyEvent anomaly;

    public AnomalyDetectedEvent(Object source, AnomalyEvent anomaly) {
        super(source);
        this.anomaly = anomaly;
    }

    public AnomalyEvent anomaly() {
        return anomaly;
    }
}
