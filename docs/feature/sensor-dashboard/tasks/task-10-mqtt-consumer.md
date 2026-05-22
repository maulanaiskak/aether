# Task 10: MQTT Consumer

**Status:** pending
**HLD Reference:** §Technical Implementation — Processing & Signal (MqttReadingConsumer), §Sequence Diagram Part 2

## Description

Wire the Paho MQTT v5 inbound adapter (`Mqttv5PahoMessageDrivenChannelAdapter`) as a Spring Integration `IntegrationFlow`. The consumer subscribes to `sensors/#`, deserializes incoming JSON payloads to `SensorReading` domain records, and emits them into a `Sinks.Many<SensorReading>` for downstream reactive processing.

## Acceptance Criteria

- [ ] `Mqttv5PahoMessageDrivenChannelAdapter` subscribes to `sensors/#` on the configured broker
- [ ] Incoming JSON deserialized to `SensorReading` via Jackson; deserialization failure logs error and skips (does not crash)
- [ ] `schemaVersion` check: if `schemaVersion > 1` (unknown future major), log a warning and drop the message
- [ ] Successfully deserialized reading emitted to `Sinks.Many<SensorReading>` with `tryEmitNext`
- [ ] Uses a distinct client ID (`aerator-01-sub`) to avoid conflict with the publisher client
- [ ] `MqttReadingConsumerTest`: publish a test JSON message to the inbound channel; verify it lands in the `Sinks.Many`

## Dependencies

- **Depends on:** Task 04 (SensorReading domain type), Task 08 (MQTT connection config pattern)
- **Blocks:** Task 12 (persistence subscribes to the Sinks.Many)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/processing/mqtt/MqttReadingConsumer.java` | Create | Inbound adapter handler |
| `src/main/java/io/aether/processing/mqtt/MqttConsumerConfig.java` | Create | Mqttv5PahoMessageDrivenChannelAdapter IntegrationFlow |
| `src/main/java/io/aether/processing/mqtt/ReadingSinkHolder.java` | Create | `@Bean Sinks.Many<SensorReading>` — shared across processing module |
| `src/test/java/io/aether/processing/mqtt/MqttReadingConsumerTest.java` | Create | Unit test via direct channel send |

## Implementation Hints

- **Inbound adapter wiring:**
  ```java
  @Bean
  public Mqttv5PahoMessageDrivenChannelAdapter mqttInboundAdapter(
          @Value("${aether.mqtt.broker-url}") String brokerUrl) {
      var options = new MqttConnectionOptions();
      options.setServerURIs(new String[]{brokerUrl});
      options.setAutomaticReconnect(true);
      var adapter = new Mqttv5PahoMessageDrivenChannelAdapter(
          options, "aerator-01-sub", "sensors/#");
      adapter.setQos(1);
      adapter.setOutputChannelName("mqttInboundChannel");
      return adapter;
  }

  @Bean
  public IntegrationFlow mqttInboundFlow(
          Mqttv5PahoMessageDrivenChannelAdapter adapter,
          MqttReadingConsumer consumer) {
      return IntegrationFlow.from(adapter)
          .handle(consumer)
          .get();
  }
  ```
- **ReadingSinkHolder:**
  ```java
  @Bean
  public Sinks.Many<SensorReading> readingSink() {
      return Sinks.many().multicast().onBackpressureBuffer(256);
  }
  ```
- **Consumer handler:**
  ```java
  public void handle(Message<?> message) {
      try {
          var reading = objectMapper.readValue((String) message.getPayload(), SensorReading.class);
          if (reading.schemaVersion() > SUPPORTED_SCHEMA_VERSION) { log.warn(...); return; }
          readingSink.tryEmitNext(reading);
      } catch (JsonProcessingException e) {
          log.error("Failed to deserialize MQTT reading: {}", e.getMessage());
      }
  }
  ```
- **Key consideration:** `Sinks.Many` with `multicast().onBackpressureBuffer()` is correct for multiple downstream subscribers (validation pipeline, SSE broadcaster). If only one subscriber is expected at a time, `unicast()` is simpler but breaks if two subscribers attach.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
