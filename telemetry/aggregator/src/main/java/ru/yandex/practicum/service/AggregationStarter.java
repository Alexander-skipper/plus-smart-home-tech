package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;
    private final AggregationService aggregationService;

    @Value("${kafka.topics.sensors-in}")
    private String sensorsInTopic;

    @Value("${kafka.topics.snapshots-out}")
    private String snapshotsOutTopic;

    public void start() {
        try {
            consumer.subscribe(List.of(sensorsInTopic));
            log.info("Подписка на топик {} выполнена", sensorsInTopic);

            Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                if (records.isEmpty()) {
                    continue;
                }

                log.debug("Получено {} событий", records.count());

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();

                    if (event == null) {
                        log.warn("Получено пустое событие");
                        continue;
                    }

                    log.debug("Обработка события: hubId={}, sensorId={}, timestamp={}",
                            event.getHubId(), event.getId(), event.getTimestamp());

                    Optional<SensorsSnapshotAvro> snapshotOpt = aggregationService.updateState(event);

                    if (snapshotOpt.isPresent()) {
                        SensorsSnapshotAvro snapshot = snapshotOpt.get();
                        ProducerRecord<String, SensorsSnapshotAvro> producerRecord =
                                new ProducerRecord<>(snapshotsOutTopic, snapshot.getHubId(), snapshot);

                        producer.send(producerRecord, (metadata, exception) -> {
                            if (exception != null) {
                                log.error("Ошибка отправки снапшота в Kafka", exception);
                            } else {
                                log.info("Снапшот отправлен: hubId={}, topic={}, partition={}, offset={}",
                                        snapshot.getHubId(), metadata.topic(),
                                        metadata.partition(), metadata.offset());
                            }
                        });
                    }

                    currentOffsets.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );
                }

                if (!currentOffsets.isEmpty()) {
                    consumer.commitSync(currentOffsets);
                    currentOffsets.clear();
                    log.debug("Смещения зафиксированы");
                }
            }

        } catch (WakeupException ignored) {
            log.info("Получен сигнал завершения работы");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        try {
            log.info("Завершение работы...");

            producer.flush();

            if (!consumer.assignment().isEmpty()) {
                consumer.commitSync();
            }

        } catch (Exception e) {
            log.error("Ошибка при завершении работы", e);
        } finally {
            log.info("Закрываем консьюмер");
            consumer.close();
            log.info("Закрываем продюсер");
            producer.close();
            log.info("Завершение работы успешно выполнено");
        }
    }
}
