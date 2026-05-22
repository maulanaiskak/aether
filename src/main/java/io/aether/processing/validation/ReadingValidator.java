package io.aether.processing.validation;

import io.aether.domain.SensorReading;

public interface ReadingValidator {
    SensorReading validate(SensorReading reading);
}
