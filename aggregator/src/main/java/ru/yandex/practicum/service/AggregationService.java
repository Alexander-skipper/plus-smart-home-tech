package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AggregationService {

    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(hubId,
                k -> SensorsSnapshotAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(Instant.now())
                        .setSensorsState(new HashMap<>())
                        .build());

        SensorStateAvro oldState = snapshot.getSensorsState().get(sensorId);

        if (oldState != null) {
            if (oldState.getTimestamp().isAfter(event.getTimestamp())) {
                log.debug("Событие устарело для датчика {} (текущее: {}, новое: {})",
                        sensorId, oldState.getTimestamp(), event.getTimestamp());
                return Optional.empty();
            }

            if (oldState.getData().equals(event.getPayload())) {
                log.debug("Данные не изменились для датчика {}", sensorId);
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        snapshot.getSensorsState().put(sensorId, newState);
        snapshot.setTimestamp(event.getTimestamp());

        log.debug("Снапшот обновлен для хаба {}, датчик {}", hubId, sensorId);

        return Optional.of(snapshot);
    }
}
