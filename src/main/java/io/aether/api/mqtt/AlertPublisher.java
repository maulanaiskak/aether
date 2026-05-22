package io.aether.api.mqtt;

import io.aether.domain.AnomalyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertPublisher.class);

    private final MessageChannel alertOutboundChannel;
    private final ObjectMapper objectMapper;

    public AlertPublisher(
            @Qualifier("alertOutboundChannel") MessageChannel alertOutboundChannel,
            ObjectMapper objectMapper) {
        this.alertOutboundChannel = alertOutboundChannel;
        this.objectMapper = objectMapper;
    }

    public void publish(AnomalyEvent event) {
        try {
            String topic = "alerts/" + event.location() + "/" + event.metric().name().toLowerCase();
            String payload = objectMapper.writeValueAsString(event);
            var message = MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .build();
            alertOutboundChannel.send(message);
        } catch (JacksonException e) {
            log.error("Failed to serialize AnomalyEvent for MQTT: {}", e.getMessage());
        }
    }
}
