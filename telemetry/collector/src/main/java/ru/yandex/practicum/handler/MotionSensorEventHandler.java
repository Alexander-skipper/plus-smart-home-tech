package ru.yandex.practicum.handler;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.MotionSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.service.KafkaEventProducer;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class MotionSensorEventHandler implements SensorEventHandler {

    private final KafkaEventProducer kafkaProducer;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.MOTION_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        MotionSensorProto motionSensor = event.getMotionSensor();

        MotionSensorAvro payload = MotionSensorAvro.newBuilder()
                .setLinkQuality(motionSensor.getLinkQuality())
                .setMotion(motionSensor.getMotion())
                .setVoltage(motionSensor.getVoltage())
                .build();

        SensorEventAvro avroEvent = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(convertTimestamp(event.getTimestamp()))
                .setPayload(payload)
                .build();

        kafkaProducer.sendSensorEvent(avroEvent);
    }

    private Instant convertTimestamp(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
