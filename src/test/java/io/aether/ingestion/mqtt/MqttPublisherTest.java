package io.aether.ingestion.mqtt;

import io.aether.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.support.MqttHeaders;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MqttPublisherTest {

    @Test
    void publishSendsMessageToChannel() {
        var channel = new QueueChannel();
        var publisher = new MqttPublisher(channel, new ObjectMapper());

        var reading = new SensorReading(
                SensorId.parse("open-meteo:surabaya:pm2_5"),
                1, "surabaya", Metric.PM2_5, "µg/m³",
                25.0, Instant.now(), Instant.now(), "open-meteo", Quality.ok()
        );

        publisher.publish(reading);

        var message = channel.receive(500);
        assertThat(message).isNotNull();
        assertThat(message.getHeaders().get(MqttHeaders.TOPIC, String.class))
                .isEqualTo("sensors/surabaya/pm2_5");
        assertThat(message.getPayload().toString()).contains("surabaya");
    }
}
