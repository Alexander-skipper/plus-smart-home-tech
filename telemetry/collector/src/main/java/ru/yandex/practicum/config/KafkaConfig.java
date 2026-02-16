package ru.yandex.practicum.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.*;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.serialization.HubEventAvroSerializer;
import ru.yandex.practicum.serialization.SensorEventAvroSerializer;

import java.util.*;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> props() {
        Map<String, Object> map = new HashMap<>();
        map.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        map.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return map;
    }

    @Bean
    public KafkaTemplate<String, SensorEventAvro> sensorTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                props(), new StringSerializer(), new SensorEventAvroSerializer()));
    }

    @Bean
    public KafkaTemplate<String, HubEventAvro> hubTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                props(), new StringSerializer(), new HubEventAvroSerializer()));
    }
}
