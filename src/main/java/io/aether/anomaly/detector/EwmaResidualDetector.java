package io.aether.anomaly.detector;

import io.aether.domain.QualityStatus;
import io.aether.domain.SensorReading;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class EwmaResidualDetector implements AnomalyDetector {

    private static final double ALPHA = 0.3;
    private static final double THRESHOLD = 3.0;

    @Override
    public AnomalyResult detect(List<SensorReading> window, SensorReading current) {
        if (current.quality().status() == QualityStatus.REJECTED) return AnomalyResult.notAnomalous();
        if (current.value() == null) return AnomalyResult.notAnomalous();

        var values = window.stream().map(SensorReading::value).filter(Objects::nonNull).toList();
        if (values.size() < 5) return AnomalyResult.notAnomalous();

        double ewma = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            ewma = ALPHA * values.get(i) + (1 - ALPHA) * ewma;
        }
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stddev = Math.sqrt(values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0));

        double residual = Math.abs(current.value() - ewma);
        double score = stddev == 0 ? 0 : residual / stddev;
        return new AnomalyResult(score > THRESHOLD, score, "EWMA_RESIDUAL");
    }
}
