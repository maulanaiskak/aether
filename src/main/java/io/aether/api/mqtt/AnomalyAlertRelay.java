package io.aether.api.mqtt;

import io.aether.domain.event.AnomalyDetectedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AnomalyAlertRelay {

    private final AlertPublisher alertPublisher;

    public AnomalyAlertRelay(AlertPublisher alertPublisher) {
        this.alertPublisher = alertPublisher;
    }

    @EventListener
    public void onAnomaly(AnomalyDetectedEvent event) {
        alertPublisher.publish(event.anomaly());
    }
}
