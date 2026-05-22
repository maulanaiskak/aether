package io.aether.insight;

import io.aether.domain.InsightContext;
import io.aether.domain.Metric;
import io.aether.domain.QualityFlag;
import io.aether.domain.SensorReading;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

@Component
public class RuleBasedInsightProvider implements InsightProvider {

    @Override
    public Mono<Insight> generate(InsightContext ctx) {
        return Mono.fromCallable(() -> buildInsight(ctx));
    }

    private Insight buildInsight(InsightContext ctx) {
        var parts = new ArrayList<String>();

        applyAnomalyRule(ctx, parts);
        applyPm25TrendRule(ctx, parts);
        applyForecastRule(ctx, parts);
        applyAqiRule(ctx, parts);
        applyImputedRule(ctx, parts);

        if (parts.isEmpty()) parts.add("All metrics are within normal range.");
        return new Insight(ctx.location().name(), ctx.asOf(), String.join(" ", parts), "rule-based");
    }

    private void applyAnomalyRule(InsightContext ctx, List<String> parts) {
        if (!ctx.activeAnomalies().isEmpty()) {
            var a = ctx.activeAnomalies().get(0);
            parts.add(String.format("Anomaly detected on %s via %s (score=%.2f).",
                    a.metric().name(), a.method(), a.score()));
        }
    }

    private void applyPm25TrendRule(InsightContext ctx, List<String> parts) {
        var pm25 = ctx.recentWindow().stream()
                .filter(r -> r.metric() == Metric.PM2_5 && r.value() != null)
                .toList();
        if (pm25.size() < 8) return;

        OptionalDouble recent = pm25.subList(pm25.size() - 4, pm25.size()).stream()
                .mapToDouble(SensorReading::value).average();
        OptionalDouble prior = pm25.subList(pm25.size() - 8, pm25.size() - 4).stream()
                .mapToDouble(SensorReading::value).average();

        if (recent.isPresent() && prior.isPresent() && prior.getAsDouble() > 0) {
            double pct = (recent.getAsDouble() - prior.getAsDouble()) / prior.getAsDouble() * 100;
            if (pct > 10) parts.add(String.format("PM2.5 trending up %.0f%%.", pct));
        }
    }

    private void applyForecastRule(InsightContext ctx, List<String> parts) {
        var current = ctx.recentWindow().stream()
                .filter(r -> r.metric() == Metric.PM2_5 && r.value() != null)
                .reduce((a, b) -> b).map(SensorReading::value);
        if (current.isEmpty() || ctx.forecast().isEmpty()) return;

        // check 8h forecast horizon
        var point8h = ctx.forecast().stream()
                .skip(Math.min(7, ctx.forecast().size() - 1))
                .findFirst();
        point8h.ifPresent(fp -> {
            double pct = (fp.predicted() - current.get()) / current.get() * 100;
            if (pct > 15) parts.add(String.format("Forecast shows PM2.5 increasing %.0f%% in 8h.", pct));
        });
    }

    private void applyAqiRule(InsightContext ctx, List<String> parts) {
        ctx.recentWindow().stream()
                .filter(r -> r.metric() == Metric.PM2_5 && r.value() != null)
                .reduce((a, b) -> b)
                .ifPresent(r -> parts.add("Current AQI: " + aqiLabel(r.value()) + "."));
    }

    private void applyImputedRule(InsightContext ctx, List<String> parts) {
        long imputed = ctx.recentWindow().stream()
                .filter(r -> r.quality().flags().contains(QualityFlag.IMPUTED))
                .count();
        long total = ctx.recentWindow().size();
        if (total > 0 && (double) imputed / total > 0.30) {
            parts.add(String.format("Note: %.0f%% of readings in window are imputed.", (double) imputed / total * 100));
        }
    }

    static String aqiLabel(double pm25) {
        if (pm25 < 12) return "Good";
        if (pm25 < 35.4) return "Moderate";
        if (pm25 < 55.4) return "Unhealthy for Sensitive Groups";
        if (pm25 < 150.4) return "Unhealthy";
        if (pm25 < 250.4) return "Very Unhealthy";
        return "Hazardous";
    }
}
