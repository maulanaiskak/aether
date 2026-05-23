# Task 08: MQTT Publisher

**Status:** pending
**HLD Reference:** §Technical Implementation — Ingestion Gateway (MqttPublisher), §API Layer (MQTT alert publisher)

## Description

Wire the Paho MQTT v5 outbound adapter as a Spring Integration `IntegrationFlow`. The publisher is used by two modules: `ingestion` sends `sensors/{loc}/{metric}` readings; `api` sends `alerts/{loc}/{metric}` anomaly events. Both use the same Mosquitto broker but different topic prefixes. Implement them as two separate `Mqttv5PahoMessageHandler` beans to maintain module independence.

## Acceptance Criteria

- [ ] `MqttPublisher` bean in `io.aether.ingestion` wraps `Mqttv5PahoMessageHandler` for `sensors/#` outbound
- [ ] `AlertPublisher` bean in `io.aether.api` wraps a second `Mqttv5PahoMessageHandler` for `alerts/#` outbound
- [ ] Both use `client-id` from config (`aether-01`); they must use different client IDs to avoid broker conflict — append `-pub` and `-alert` suffixes respectively
- [ ] QoS 1 for all publishes
- [ ] Payload is Jackson-serialized `SensorReading` (or `AnomalyEvent`) JSON
- [ ] `MqttPublisherTest` verifies that `publish(SensorReading)` dispatches a message to the integration channel (using in-memory channel, no Mosquitto container required)
- [ ] Application starts without errors when Mosquitto is not available at startup — MQTT connection is deferred (retry on first use)

## Dependencies

- **Depends on:** Task 04 (SensorReading domain type), Task 01 (Spring Integration MQTT on classpath)
- **Blocks:** Task 09 (scheduler calls MqttPublisher), Task 10 (consumer uses same broker), Task 23 (alert publisher)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/ingestion/mqtt/MqttPublisher.java` | Create | Sends sensor readings to sensors/# |
| `src/main/java/io/aether/ingestion/mqtt/MqttIntegrationConfig.java` | Create | IntegrationFlow + Mqttv5PahoMessageHandler bean for ingestion |
| `src/main/java/io/aether/api/mqtt/AlertPublisher.java` | Create | Sends anomaly alerts to alerts/# |
| `src/main/java/io/aether/api/mqtt/AlertMqttConfig.java` | Create | IntegrationFlow + Mqttv5PahoMessageHandler bean for api |
| `src/test/java/io/aether/ingestion/mqtt/MqttPublisherTest.java` | Create | Unit test with in-memory channel |

## Implementation Hints

- **Mqttv5PahoMessageHandler wiring:**
  ```java
  @Bean
  public Mqttv5PahoMessageHandler mqttOutboundHandler(
          @Value("${aether.mqtt.broker-url}") String brokerUrl) {
      var options = new MqttConnectionOptions();
      options.setServerURIs(new String[]{brokerUrl});
      options.setAutomaticReconnect(true);
      var handler = new Mqttv5PahoMessageHandler(options, "aether-01-pub");
      handler.setDefaultQos(1);
      handler.setAsync(true);
      return handler;
  }

  @Bean
  public IntegrationFlow mqttOutboundFlow(Mqttv5PahoMessageHandler handler) {
      return IntegrationFlow.from(mqttOutboundChannel())
          .handle(handler)
          .get();
  }

  @Bean
  public MessageChannel mqttOutboundChannel() {
      return new DirectChannel();
  }
  ```
- **MqttPublisher.publish:**
  ```java
  public void publish(SensorReading reading) {
      String topic = "sensors/" + reading.location().name() + "/" + reading.metric().name().toLowerCase();
      String payload = objectMapper.writeValueAsString(reading);
      var message = MessageBuilder.withPayload(payload)
          .setHeader(MqttHeaders.TOPIC, topic)
          .build();
      mqttOutboundChannel.send(message);
  }
  ```
- **Key consideration:** Use distinct client IDs for publisher and subscriber — Mosquitto rejects two connections with the same client ID.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
