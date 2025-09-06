package com.alien.bank.management.system.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevFlywayConfig {

    @Bean
    public FlywayMigrationStrategy devFlywayMigrationStrategy() {
        return new FlywayMigrationStrategy() {
            @Override
            public void migrate(Flyway flyway) {
                try {
                    flyway.repair();
                } catch (Exception ignored) {
                }
                flyway.migrate();
            }
        };
    }
}


