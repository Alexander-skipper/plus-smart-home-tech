package ru.yandex.practicum.service.analyzer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    @Getter
    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final ScenarioAnalyzer scenarioAnalyzer;

    @Value("${kafka.topics.snapshots-in}")
    private String snapshotsInTopic;

    public void start() {
        try {
            consumer.subscribe(List.of(snapshotsInTopic));
            log.info("SnapshotProcessor подписан на топик {}", snapshotsInTopic);

            Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    SensorsSnapshotAvro snapshot = record.value();
                    if (snapshot != null) {
                        log.debug("Получен снапшот для хаба {}, timestamp: {}",
                                snapshot.getHubId(), snapshot.getTimestamp());
                        scenarioAnalyzer.analyze(snapshot);
                    }

                    currentOffsets.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );
                }

                if (!currentOffsets.isEmpty()) {
                    consumer.commitSync(currentOffsets);
                    currentOffsets.clear();
                    log.debug("Смещения для снапшотов зафиксированы");
                }
            }

        } catch (WakeupException e) {
            log.info("SnapshotProcessor получил сигнал завершения");
        } catch (Exception e) {
            log.error("Ошибка в SnapshotProcessor", e);
        } finally {
            consumer.close();
            log.info("SnapshotProcessor закрыт");
        }
    }
}