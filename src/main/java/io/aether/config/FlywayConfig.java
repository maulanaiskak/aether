package io.aether.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FlywayConfig {

    @Bean(initMethod = "migrate")
    Flyway flyway(
            @Value("${spring.flyway.url}") String url,
            @Value("${spring.flyway.user:aether}") String user,
            @Value("${spring.flyway.password:aether}") String pw) {
        return Flyway.configure()
                .dataSource(url, user, pw)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }
}
