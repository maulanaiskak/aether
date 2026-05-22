# Task 19: Insight Engine

**Status:** pending
**HLD Reference:** §Technical Implementation — Insight Engine (`io.aether.insight`), §Functional Requirements FR-12

## Description

Implement the `InsightProvider` interface, `InsightContext` builder, and `RuleBasedInsightProvider`. The rule-based provider produces deterministic templated summaries from threshold + trend-slope + anomaly-event rules. No external dependencies. The LLM stub is also created to allow config-driven selection in v2.

## Acceptance Criteria

- [ ] `InsightProvider` interface: `Mono<Insight> generate(InsightContext ctx)`
- [ ] `Insight` record: `location`, `asOf`, `summary` (String), `provider` (String)
- [ ] `InsightContext` contains: `recentWindow` (last 12 validated readings), `forecast` (next 24h `ForecastPoint` list), `activeAnomalies` (anomalies in last 2h), `location`, `asOf`
- [ ] `RuleBasedInsightProvider` rules (at minimum):
  - If `activeAnomalies` non-empty: include anomaly description with method + score
  - PM2.5 trend: compare mean of last 4 readings vs prior 4; if >10% increase → "trending up X%"
  - Forecast: if predicted PM2.5 in 8h > current by >15% → note upcoming degradation
  - AQI classification: derive US AQI label from current PM2.5 value (Good/Moderate/Unhealthy...)
  - Imputed readings in window: note if >30% of window is IMPUTED
- [ ] `LlmInsightProvider` stub: throws `UnsupportedOperationException("LLM insight is a v2 feature")`
- [ ] Provider selected via `${aether.insight.provider}` config; `rule-based` is default
- [ ] `RuleBasedInsightProviderTest`: inject context with known anomaly, known trend; assert summary contains expected phrases

## Dependencies

- **Depends on:** Task 04 (InsightContext, ForecastPoint, AnomalyEvent domain types), Task 05 (repositories to build context)
- **Blocks:** Task 23 (insight endpoint calls provider)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/io/aether/insight/InsightProvider.java` | Create | Interface |
| `src/main/java/io/aether/insight/Insight.java` | Create | Result record |
| `src/main/java/io/aether/insight/InsightContextBuilder.java` | Create | Builds InsightContext from DB queries |
| `src/main/java/io/aether/insight/RuleBasedInsightProvider.java` | Create | Deterministic rule engine |
| `src/main/java/io/aether/insight/LlmInsightProvider.java` | Create | v2 stub |
| `src/main/java/io/aether/insight/InsightConfig.java` | Create | Bean selection by config property |
| `src/test/java/io/aether/insight/RuleBasedInsightProviderTest.java` | Create | Unit tests with injected contexts |

## Implementation Hints

- **AQI from PM2.5 (US EPA breakpoints):**
  ```java
  static String aqiLabel(double pm25) {
      if (pm25 < 12)  return "Good";
      if (pm25 < 35.4) return "Moderate";
      if (pm25 < 55.4) return "Unhealthy for Sensitive Groups";
      if (pm25 < 150.4) return "Unhealthy";
      if (pm25 < 250.4) return "Very Unhealthy";
      return "Hazardous";
  }
  ```
- **Config-driven provider selection:**
  ```java
  @Bean
  public InsightProvider insightProvider(
          @Value("${aether.insight.provider:rule-based}") String type,
          RuleBasedInsightProvider rulesBased,
          LlmInsightProvider llm) {
      return "llm".equals(type) ? llm : rulesBased;
  }
  ```
- **Key consideration:** `InsightContextBuilder` makes DB queries (fetch recent readings, forecast, anomalies). These must be `Mono`-based and composed reactively before passing to the (sync-inside-Mono) rule engine. Do not block in a request handler thread.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
