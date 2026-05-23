package io.aether.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MqttHeartbeatPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttHeartbeatPublisher.class);

    private final MessageChannel mqttOutboundChannel;

    public MqttHeartbeatPublisher(@Qualifier("mqttOutboundChannel") MessageChannel mqttOutboundChannel) {
        this.mqttOutboundChannel = mqttOutboundChannel;
    }

    @Scheduled(fixedRate = 30_000)
    public void publishHeartbeat() {
        try {
            var message = MessageBuilder.withPayload("{\"ts\":\"" + Instant.now() + "\"}")
                    .setHeader(MqttHeaders.TOPIC, "aether/system/heartbeat")
                    .build();
            mqttOutboundChannel.send(message);
        } catch (Exception e) {
            log.debug("Heartbeat publish failed (broker may be unavailable): {}", e.getMessage());
        }
    }
}
