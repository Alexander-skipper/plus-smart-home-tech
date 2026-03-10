package ru.yandex.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.TemperatureSensorProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;
import ru.yandex.practicum.service.KafkaEventProducer;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemperatureSensorEventHandler implements SensorEventHandler {

    private final KafkaEventProducer kafkaProducer;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.TEMPERATURE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        TemperatureSensorProto temperatureSensor = event.getTemperatureSensor();

        TemperatureSensorAvro payload = TemperatureSensorAvro.newBuilder()
                .setTemperatureC(temperatureSensor.getTemperatureC())
                .setTemperatureF(temperatureSensor.getTemperatureF())
                .build();

        SensorEventAvro avroEvent = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(Instant.ofEpochSecond(
                        event.getTimestamp().getSeconds(),
                        event.getTimestamp().getNanos()))
                .setPayload(payload)
                .build();

        kafkaProducer.sendSensorEvent(avroEvent);
        log.info("Обработано событие температурного сенсора: hubId={}, sensorId={}, tempC={}",
                event.getHubId(), event.getId(), temperatureSensor.getTemperatureC());
    }
}