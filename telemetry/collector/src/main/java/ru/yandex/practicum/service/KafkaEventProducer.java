package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.mapper.*;
import ru.yandex.practicum.model.base.HubEvent;
import ru.yandex.practicum.model.base.SensorEvent;

@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, SensorEventAvro> sensorTemplate;
    private final KafkaTemplate<String, HubEventAvro> hubTemplate;
    private final SensorEventMapper sensorMapper;
    private final HubEventMapper hubMapper;

    @Value("${kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${kafka.topics.hubs}")
    private String hubsTopic;

    public void sendSensorEvent(SensorEvent event) {
        sensorTemplate.send(sensorsTopic, event.getId(), sensorMapper.toAvro(event));
    }

    public void sendHubEvent(HubEvent event) {
        hubTemplate.send(hubsTopic, event.getHubId(), hubMapper.toAvro(event));
    }
}
