package io.aether.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
class FlywayConfig {

    @Bean
    DataSource flywayDataSource(
            @Value("${spring.flyway.url}") String url,
            @Value("${spring.flyway.user:aether}") String user,
            @Value("${spring.flyway.password:aether}") String pw) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(pw);
        return ds;
    }
}
