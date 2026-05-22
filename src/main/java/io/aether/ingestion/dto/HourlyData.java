package io.aether.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HourlyData(
        List<String> time,
        @JsonProperty("temperature_2m") List<Double> temperature2m,
        @JsonProperty("relative_humidity_2m") List<Double> relativeHumidity2m,
        @JsonProperty("wind_speed_10m") List<Double> windSpeed10m,
        @JsonProperty("surface_pressure") List<Double> surfacePressure,
        @JsonProperty("pm2_5") List<Double> pm25,
        @JsonProperty("pm10") List<Double> pm10,
        @JsonProperty("ozone") List<Double> ozone,
        @JsonProperty("nitrogen_dioxide") List<Double> nitrogenDioxide,
        @JsonProperty("sulphur_dioxide") List<Double> sulphurDioxide,
        @JsonProperty("carbon_monoxide") List<Double> carbonMonoxide,
        @JsonProperty("us_aqi") List<Double> usAqi,
        @JsonProperty("european_aqi") List<Double> europeanAqi
) {}
