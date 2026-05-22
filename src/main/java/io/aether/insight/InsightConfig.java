package io.aether.insight;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class InsightConfig {

    @Bean
    InsightProvider insightProvider(
            @Value("${aether.insight.provider:rule-based}") String type,
            RuleBasedInsightProvider rulesBased,
            LlmInsightProvider llm) {
        return "llm".equals(type) ? llm : rulesBased;
    }
}
