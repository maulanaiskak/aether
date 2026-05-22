package io.aether.processing.mqtt;

import io.aether.domain.SensorReading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Sinks;

@Configuration
class ReadingSinkHolder {

    @Bean
    Sinks.Many<SensorReading> readingSink() {
        return Sinks.many().multicast().onBackpressureBuffer(256);
    }
}
