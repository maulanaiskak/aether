package io.aether.insight;

import io.aether.domain.InsightContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class LlmInsightProvider implements InsightProvider {

    @Override
    public Mono<Insight> generate(InsightContext ctx) {
        return Mono.error(new UnsupportedOperationException("LLM insight is a v2 feature"));
    }
}
