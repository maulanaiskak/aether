package io.aether.insight;

import io.aether.domain.InsightContext;
import reactor.core.publisher.Mono;

public interface InsightProvider {
    Mono<Insight> generate(InsightContext ctx);
}
