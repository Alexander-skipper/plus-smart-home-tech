package ru.yandex.practicum.config;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.serialization.HubEventAvroSerializer;
import ru.yandex.practicum.serialization.SensorEventAvroSerializer;

import java.util.*;

@Configuration
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Properties getBaseProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return props;
    }

    @Bean
    public KafkaProducer<String, SensorEventAvro> sensorEventKafkaProducer() {
        Properties props = getBaseProperties();
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                SensorEventAvroSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    @Bean
    public KafkaProducer<String, HubEventAvro> hubEventKafkaProducer() {
        Properties props = getBaseProperties();
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                HubEventAvroSerializer.class.getName());
        return new KafkaProducer<>(props);
    }
}
