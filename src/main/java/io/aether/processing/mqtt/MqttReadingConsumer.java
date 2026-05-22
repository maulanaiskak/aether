package io.aether.processing.mqtt;

import io.aether.domain.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class MqttReadingConsumer implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MqttReadingConsumer.class);
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ObjectMapper objectMapper;
    private final Sinks.Many<SensorReading> readingSink;

    public MqttReadingConsumer(ObjectMapper objectMapper, Sinks.Many<SensorReading> readingSink) {
        this.objectMapper = objectMapper;
        this.readingSink = readingSink;
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            var reading = objectMapper.readValue(message.getPayload().toString(), SensorReading.class);
            if (reading.schemaVersion() > SUPPORTED_SCHEMA_VERSION) {
                log.warn("Dropping MQTT reading with unsupported schemaVersion={}", reading.schemaVersion());
                return;
            }
            readingSink.tryEmitNext(reading);
        } catch (JacksonException e) {
            log.error("Failed to deserialize MQTT reading: {}", e.getMessage());
        }
    }
}
