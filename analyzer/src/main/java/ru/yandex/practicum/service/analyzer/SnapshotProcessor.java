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

    private long processedCount = 0;
    private long startTime = System.currentTimeMillis();

    public void start() {
        try {
            consumer.subscribe(List.of(snapshotsInTopic));
            log.info("SnapshotProcessor подписан на топик {}", snapshotsInTopic);

            Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(100));

                if (records.isEmpty()) {
                    continue;
                }

                int recordCount = records.count();
                log.info("Получено {} снапшотов за poll", recordCount);

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    SensorsSnapshotAvro snapshot = record.value();
                    if (snapshot != null) {
                        processSnapshot(snapshot);
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

        } catch (WakeupException e) {
            log.info("SnapshotProcessor получил сигнал завершения");
        } catch (Exception e) {
            log.error("Ошибка в SnapshotProcessor", e);
        } finally {
            consumer.close();
            log.info("SnapshotProcessor закрыт. Всего обработано: {}", processedCount);
        }
    }

    private void processSnapshot(SensorsSnapshotAvro snapshot) {
        long snapshotStart = System.currentTimeMillis();

        try {
            log.info(">>> ОБРАБОТКА СНАПШОТА для хаба: {}, timestamp: {}, датчиков: {}",
                    snapshot.getHubId(), snapshot.getTimestamp(), snapshot.getSensorsState().size());

            scenarioAnalyzer.analyze(snapshot);

            long duration = System.currentTimeMillis() - snapshotStart;
            processedCount++;

            log.info("СНАПШОТ ОБРАБОТАН для хаба: {}, время: {} мс, всего обработано: {}",
                    snapshot.getHubId(), duration, processedCount);

        } catch (Exception e) {
            log.error("ОШИБКА при анализе снапшота для хаба {}", snapshot.getHubId(), e);
        }
    }
}