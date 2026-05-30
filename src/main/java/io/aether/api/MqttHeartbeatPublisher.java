package io.aether.api;

import io.aether.ingestion.mqtt.MqttPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MqttHeartbeatPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttHeartbeatPublisher.class);

    private final MqttPublisher mqttPublisher;

    public MqttHeartbeatPublisher(MqttPublisher mqttPublisher) {
        this.mqttPublisher = mqttPublisher;
    }

    @Scheduled(fixedRate = 30_000)
    public void publishHeartbeat() {
        mqttPublisher.publishRaw("aether/system/heartbeat", "{\"ts\":\"" + Instant.now() + "\"}");
    }
}
