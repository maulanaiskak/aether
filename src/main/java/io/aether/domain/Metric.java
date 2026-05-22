package io.aether.domain;

public enum Metric {
    TEMPERATURE("°C"),
    HUMIDITY("%"),
    WIND_SPEED("km/h"),
    PRESSURE("hPa"),
    PM2_5("µg/m³"),
    PM10("µg/m³"),
    O3("µg/m³"),
    NO2("µg/m³"),
    SO2("µg/m³"),
    CO("µg/m³"),
    US_AQI("AQI"),
    EU_AQI("AQI");

    private final String unit;

    Metric(String unit) {
        this.unit = unit;
    }

    public String unit() {
        return unit;
    }
}
