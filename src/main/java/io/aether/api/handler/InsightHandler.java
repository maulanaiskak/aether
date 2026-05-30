package io.aether.api.handler;

import io.aether.api.dto.InsightRequestDto;
import io.aether.api.dto.InsightResponseDto;
import io.aether.config.AetherProperties;
import io.aether.domain.Location;
import io.aether.insight.InsightContextBuilder;
import io.aether.insight.InsightProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InsightHandler {

    private final InsightProvider insightProvider;
    private final InsightContextBuilder contextBuilder;
    private final Map<String, Location> locationMap;

    public InsightHandler(
            InsightProvider insightProvider,
            InsightContextBuilder contextBuilder,
            AetherProperties properties) {
        this.insightProvider = insightProvider;
        this.contextBuilder = contextBuilder;
        this.locationMap = properties.getLocationList().stream()
                .collect(Collectors.toMap(Location::name, Function.identity()));
    }

    public Mono<ServerResponse> generateInsight(ServerRequest request) {
        return request.bodyToMono(InsightRequestDto.class)
                .flatMap(req -> {
                    var location = locationMap.get(req.location().toLowerCase());
                    if (location == null) {
                        return Mono.<InsightResponseDto>error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Unknown location: " + req.location()));
                    }
                    return contextBuilder.build(location)
                            .flatMap(insightProvider::generate)
                            .map(i -> {
                                var insight = new InsightResponseDto.InsightDto("summary", "INFO", "Analysis", i.summary());
                                return new InsightResponseDto(
                                        i.location(),
                                        req.metric(),
                                        i.provider(),
                                        java.util.List.of(insight),
                                        i.asOf().toString());
                            });
                })
                .flatMap(dto -> ServerResponse.ok().bodyValue(dto))
                .onErrorResume(ResponseStatusException.class,
                        e -> ServerResponse.status(e.getStatusCode()).bodyValue(e.getReason()));
    }
}
