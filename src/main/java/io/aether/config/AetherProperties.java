package io.aether.config;

import io.aether.domain.Location;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "aether")
public class AetherProperties {

    private List<LocationConfig> locations = new ArrayList<>();
    private MqttConfig mqtt = new MqttConfig();
    private IngestionConfig ingestion = new IngestionConfig();
    private AnomalyConfig anomaly = new AnomalyConfig();
    private MlServiceConfig mlService = new MlServiceConfig();
    private InsightConfig insight = new InsightConfig();

    public List<Location> getLocationList() {
        return locations.stream()
                .map(l -> new Location(l.name, l.lat, l.lon))
                .toList();
    }

    public List<LocationConfig> getLocations() { return locations; }
    public void setLocations(List<LocationConfig> locations) { this.locations = locations; }
    public MqttConfig getMqtt() { return mqtt; }
    public void setMqtt(MqttConfig mqtt) { this.mqtt = mqtt; }
    public IngestionConfig getIngestion() { return ingestion; }
    public void setIngestion(IngestionConfig ingestion) { this.ingestion = ingestion; }
    public AnomalyConfig getAnomaly() { return anomaly; }
    public void setAnomaly(AnomalyConfig anomaly) { this.anomaly = anomaly; }
    public MlServiceConfig getMlService() { return mlService; }
    public void setMlService(MlServiceConfig mlService) { this.mlService = mlService; }
    public InsightConfig getInsight() { return insight; }
    public void setInsight(InsightConfig insight) { this.insight = insight; }

    public static class LocationConfig {
        private String name;
        private double lat;
        private double lon;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }
    }

    public static class MqttConfig {
        private String brokerUrl = "tcp://localhost:1883";
        private String clientId = "aether-01";
        private int qos = 1;
        public String getBrokerUrl() { return brokerUrl; }
        public void setBrokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public int getQos() { return qos; }
        public void setQos(int qos) { this.qos = qos; }
    }

    public static class IngestionConfig {
        private String pollCron = "0 0 * * * *";
        private int retryMaxAttempts = 3;
        private long retryBackoffMs = 2000;
        public String getPollCron() { return pollCron; }
        public void setPollCron(String pollCron) { this.pollCron = pollCron; }
        public int getRetryMaxAttempts() { return retryMaxAttempts; }
        public void setRetryMaxAttempts(int retryMaxAttempts) { this.retryMaxAttempts = retryMaxAttempts; }
        public long getRetryBackoffMs() { return retryBackoffMs; }
        public void setRetryBackoffMs(long retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }
    }

    public static class AnomalyConfig {
        private int windowSize = 48;
        private double zscoreThreshold = 3.0;
        private double iqrMultiplier = 1.5;
        public int getWindowSize() { return windowSize; }
        public void setWindowSize(int windowSize) { this.windowSize = windowSize; }
        public double getZscoreThreshold() { return zscoreThreshold; }
        public void setZscoreThreshold(double zscoreThreshold) { this.zscoreThreshold = zscoreThreshold; }
        public double getIqrMultiplier() { return iqrMultiplier; }
        public void setIqrMultiplier(double iqrMultiplier) { this.iqrMultiplier = iqrMultiplier; }
    }

    public static class MlServiceConfig {
        private String baseUrl = "http://ml-service:8000";
        private int forecastHorizonHours = 24;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getForecastHorizonHours() { return forecastHorizonHours; }
        public void setForecastHorizonHours(int forecastHorizonHours) { this.forecastHorizonHours = forecastHorizonHours; }
    }

    public static class InsightConfig {
        private String provider = "rule-based";
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
    }
}
