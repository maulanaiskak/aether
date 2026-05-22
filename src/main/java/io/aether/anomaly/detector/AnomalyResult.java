package io.aether.anomaly.detector;

public record AnomalyResult(boolean anomalous, double score, String method) {

    public static AnomalyResult notAnomalous() {
        return new AnomalyResult(false, 0.0, "NONE");
    }
}
