package io.aether.anomaly.detector;

import io.aether.domain.SensorReading;

import java.util.List;

public interface AnomalyDetector {
    AnomalyResult detect(List<SensorReading> window, SensorReading current);
}
