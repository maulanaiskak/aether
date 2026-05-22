package io.aether.processing;

import io.aether.domain.SensorReading;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Component
class ProcessingPipelineConfig {

    private static final Logger log = LoggerFactory.getLogger(ProcessingPipelineConfig.class);

    private final Sinks.Many<SensorReading> readingSink;
    private final ReadingPersistenceHandler handler;

    ProcessingPipelineConfig(Sinks.Many<SensorReading> readingSink, ReadingPersistenceHandler handler) {
        this.readingSink = readingSink;
        this.handler = handler;
    }

    @PostConstruct
    void startPipeline() {
        readingSink.asFlux()
                .flatMap(handler::processReading)
                .subscribe(
                        r -> {},
                        err -> log.error("Pipeline processing error", err));
    }
}
