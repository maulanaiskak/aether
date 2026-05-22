package io.aether.anomaly.detector;

import io.aether.domain.QualityStatus;
import io.aether.domain.SensorReading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class ZScoreDetector implements AnomalyDetector {

    private final double threshold;

    public ZScoreDetector(@Value("${aether.anomaly.zscore-threshold:3.0}") double threshold) {
        this.threshold = threshold;
    }

    @Override
    public AnomalyResult detect(List<SensorReading> window, SensorReading current) {
        if (current.quality().status() == QualityStatus.REJECTED) return AnomalyResult.notAnomalous();
        if (current.value() == null) return AnomalyResult.notAnomalous();

        var values = window.stream().map(SensorReading::value).filter(Objects::nonNull).toList();
        if (values.size() < 5) return AnomalyResult.notAnomalous();

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        double stddev = Math.sqrt(variance);
        double score = stddev == 0
                ? (current.value().equals(mean) ? 0 : Double.MAX_VALUE)
                : Math.abs(current.value() - mean) / stddev;
        return new AnomalyResult(score > threshold, score == Double.MAX_VALUE ? threshold + 1 : score, "ZSCORE");
    }
}
