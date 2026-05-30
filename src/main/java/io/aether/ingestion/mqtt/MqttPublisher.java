package io.aether.ingestion.mqtt;

import io.aether.domain.SensorReading;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class MqttPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttPublisher.class);

    private final String brokerUrl;
    private final ObjectMapper objectMapper;
    private MqttAsyncClient client;

    public MqttPublisher(
            @Value("${aether.mqtt.broker-url}") String brokerUrl,
            ObjectMapper objectMapper) {
        this.brokerUrl = brokerUrl;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void connect() {
        try {
            client = new MqttAsyncClient(brokerUrl, "aether-01-pub");
            var options = new MqttConnectionOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(true);
            client.connect(options);
        } catch (Exception e) {
            log.warn("MQTT initial connect failed (will retry automatically): {}", e.getMessage());
        }
    }

    @PreDestroy
    void disconnect() {
        try {
            if (client != null && client.isConnected()) client.disconnect();
        } catch (Exception ignored) {}
    }

    public void publishRaw(String topic, String payload) {
        if (client == null || !client.isConnected()) return;
        try {
            var msg = new MqttMessage(payload.getBytes());
            msg.setQos(0);
            client.publish(topic, msg);
        } catch (Exception e) {
            log.debug("MQTT raw publish failed: {}", e.getMessage());
        }
    }

    public void publish(SensorReading reading) {
        if (client == null || !client.isConnected()) {
            log.debug("MQTT not connected, skipping publish for {}/{}", reading.location(), reading.metric());
            return;
        }
        try {
            String topic = "sensors/" + reading.location() + "/" + reading.metric().name().toLowerCase();
            byte[] payload = objectMapper.writeValueAsBytes(reading);
            var msg = new MqttMessage(payload);
            msg.setQos(1);
            client.publish(topic, msg);
        } catch (JacksonException e) {
            log.error("Failed to serialize SensorReading for MQTT: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("MQTT publish failed for {}/{}: {}", reading.location(), reading.metric(), e.getMessage());
        }
    }
}
