package io.aether.api.router;

import io.aether.api.handler.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
class ApiRouterConfig {

    @Bean
    RouterFunction<ServerResponse> apiRouter(
            ReadingsHandler readingsHandler,
            ForecastHandler forecastHandler,
            AnomalyHandler anomalyHandler,
            StreamHandler streamHandler,
            InsightHandler insightHandler) {
        return RouterFunctions.route()
                .GET("/api/v1/readings", readingsHandler::queryReadings)
                .GET("/api/v1/readings/latest", readingsHandler::latestReadings)
                .GET("/api/v1/forecast", forecastHandler::getForecast)
                .GET("/api/v1/forecast/metrics", forecastHandler::getForecastMetrics)
                .GET("/api/v1/anomalies", anomalyHandler::queryAnomalies)
                .GET("/api/v1/stream/readings/{location}", streamHandler::streamReadings)
                .GET("/api/v1/stream/alerts/{location}", streamHandler::streamAlerts)
                .POST("/api/v1/insight", insightHandler::generateInsight)
                .build();
    }
}
