package ru.yandex.practicum.serialization;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumWriter;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.io.ByteArrayOutputStream;

public class SensorEventAvroSerializer implements Serializer<SensorEventAvro> {

    @Override
    public byte[] serialize(String topic, SensorEventAvro data) {
        if (data == null) return null;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            new SpecificDatumWriter<>(SensorEventAvro.class).write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}