package com.precisionpath.lab_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Injected wherever "today" matters so tests can control the date. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
