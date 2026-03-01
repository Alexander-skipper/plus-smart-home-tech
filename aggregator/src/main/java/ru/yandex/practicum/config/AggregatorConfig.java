package ru.yandex.practicum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.service.AggregationService;

@Configuration
public class AggregatorConfig {

    @Bean
    public AggregationService aggregationService() {
        return new AggregationService();
    }
}