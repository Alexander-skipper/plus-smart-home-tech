package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaProducer<String, SensorEventAvro> sensorEventProducer;
    private final KafkaProducer<String, HubEventAvro> hubEventProducer;

    @Value("${kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${kafka.topics.hubs}")
    private String hubsTopic;

    public void sendSensorEvent(SensorEventAvro event) {
        try {
            ProducerRecord<String, SensorEventAvro> record =
                    new ProducerRecord<>(sensorsTopic, event.getId().toString(), event);

            sensorEventProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Ошибка отправки события сенсора в Kafka", exception);
                } else {
                    log.debug("Событие сенсора отправлено: topic={}, partition={}, offset={}, key={}",
                            metadata.topic(), metadata.partition(), metadata.offset(), event.getId());
                }
            });
        } catch (Exception e) {
            log.error("Ошибка при отправке события сенсора", e);
            throw new RuntimeException("Ошибка отправки события сенсора в Kafka", e);
        }
    }

    public void sendHubEvent(HubEventAvro event) {
        try {
            ProducerRecord<String, HubEventAvro> record =
                    new ProducerRecord<>(hubsTopic, event.getHubId().toString(),event);

            hubEventProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Ошибка отправки события хаба в Kafka", exception);
                } else {
                    log.debug("Событие хаба отправлено: topic={}, partition={}, offset={}, key={}",
                            metadata.topic(), metadata.partition(), metadata.offset(), event.getHubId());
                }
            });
        } catch (Exception e) {
            log.error("Ошибка при отправке события хаба", e);
            throw new RuntimeException("Ошибка отправки события хаба в Kafka", e);
        }
    }
}
