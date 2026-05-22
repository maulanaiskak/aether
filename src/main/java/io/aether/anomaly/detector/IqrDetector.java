package io.aether.anomaly.detector;

import io.aether.domain.QualityStatus;
import io.aether.domain.SensorReading;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class IqrDetector implements AnomalyDetector {

    private final double multiplier;

    public IqrDetector(@Value("${aether.anomaly.iqr-multiplier:1.5}") double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public AnomalyResult detect(List<SensorReading> window, SensorReading current) {
        if (current.quality().status() == QualityStatus.REJECTED) return AnomalyResult.notAnomalous();
        if (current.value() == null) return AnomalyResult.notAnomalous();

        var values = window.stream().map(SensorReading::value).filter(Objects::nonNull).toList();
        if (values.size() < 5) return AnomalyResult.notAnomalous();

        double[] arr = values.stream().mapToDouble(Double::doubleValue).toArray();
        var pct = new Percentile();
        double q1 = pct.evaluate(arr, 25);
        double q3 = pct.evaluate(arr, 75);
        double iqr = q3 - q1;
        double lowerFence = q1 - multiplier * iqr;
        double upperFence = q3 + multiplier * iqr;
        boolean anomalous = current.value() < lowerFence || current.value() > upperFence;
        double score = anomalous
                ? Math.max(lowerFence - current.value(), current.value() - upperFence)
                : 0.0;
        return new AnomalyResult(anomalous, score, "IQR");
    }
}
