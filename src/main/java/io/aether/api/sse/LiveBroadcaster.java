package io.aether.api.sse;

import io.aether.api.dto.AnomalyEventDto;
import io.aether.api.dto.DtoMapper;
import io.aether.api.dto.SensorReadingDto;
import io.aether.domain.event.AnomalyDetectedEvent;
import io.aether.domain.event.ReadingValidatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class LiveBroadcaster {

    private final Sinks.Many<SensorReadingDto> readingSink =
            Sinks.many().multicast().onBackpressureBuffer(512);

    private final Sinks.Many<AnomalyEventDto> anomalySink =
            Sinks.many().multicast().onBackpressureBuffer(512);

    private final DtoMapper mapper;

    public LiveBroadcaster(DtoMapper mapper) {
        this.mapper = mapper;
    }

    @EventListener
    public void onReadingValidated(ReadingValidatedEvent event) {
        readingSink.tryEmitNext(mapper.toDto(event.reading()));
    }

    @EventListener
    public void onAnomalyDetected(AnomalyDetectedEvent event) {
        anomalySink.tryEmitNext(mapper.toDto(event.anomaly()));
    }

    public Flux<ServerSentEvent<SensorReadingDto>> readingStream(String location) {
        return readingSink.asFlux()
                .filter(dto -> dto.location().equals(location))
                .map(dto -> ServerSentEvent.<SensorReadingDto>builder()
                        .event("reading")
                        .data(dto)
                        .build());
    }

    public Flux<ServerSentEvent<AnomalyEventDto>> alertStream(String location) {
        return anomalySink.asFlux()
                .filter(dto -> dto.location().equals(location))
                .map(dto -> ServerSentEvent.<AnomalyEventDto>builder()
                        .event("alert")
                        .data(dto)
                        .build());
    }
}
