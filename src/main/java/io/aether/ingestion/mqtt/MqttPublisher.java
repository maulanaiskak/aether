package io.aether.ingestion.mqtt;

import io.aether.domain.SensorReading;
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
public class MqttPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttPublisher.class);

    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper;

    public MqttPublisher(
            @Qualifier("mqttOutboundChannel") MessageChannel mqttOutboundChannel,
            ObjectMapper objectMapper) {
        this.mqttOutboundChannel = mqttOutboundChannel;
        this.objectMapper = objectMapper;
    }

    public void publish(SensorReading reading) {
        try {
            String topic = "sensors/" + reading.location() + "/" + reading.metric().name().toLowerCase();
            String payload = objectMapper.writeValueAsString(reading);
            var message = MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .build();
            mqttOutboundChannel.send(message);
        } catch (JacksonException e) {
            log.error("Failed to serialize SensorReading for MQTT: {}", e.getMessage());
        }
    }
}
