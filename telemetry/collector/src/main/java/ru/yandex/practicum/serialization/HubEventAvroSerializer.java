package ru.yandex.practicum.serialization;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumWriter;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.io.ByteArrayOutputStream;

public class HubEventAvroSerializer implements Serializer<HubEventAvro> {

    @Override
    public byte[] serialize(String topic, HubEventAvro data) {
        if (data == null) return null;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            new SpecificDatumWriter<>(HubEventAvro.class).write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
