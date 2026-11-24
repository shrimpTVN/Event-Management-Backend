package com.ddd.application.service.event.impl;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecommendationClockConfiguration {
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock recommendationClock() {
        return Clock.systemUTC();
    }
}
