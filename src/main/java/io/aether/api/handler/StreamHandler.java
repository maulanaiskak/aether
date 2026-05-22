package io.aether.api.handler;

import io.aether.api.dto.AnomalyEventDto;
import io.aether.api.dto.SensorReadingDto;
import io.aether.api.sse.LiveBroadcaster;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class StreamHandler {

    private final LiveBroadcaster broadcaster;

    public StreamHandler(LiveBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public Mono<ServerResponse> streamReadings(ServerRequest request) {
        var location = request.pathVariable("location");
        return ServerResponse.ok()
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(broadcaster.readingStream(location),
                        new ParameterizedTypeReference<ServerSentEvent<SensorReadingDto>>() {});
    }

    public Mono<ServerResponse> streamAlerts(ServerRequest request) {
        var location = request.pathVariable("location");
        return ServerResponse.ok()
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(broadcaster.alertStream(location),
                        new ParameterizedTypeReference<ServerSentEvent<AnomalyEventDto>>() {});
    }
}
