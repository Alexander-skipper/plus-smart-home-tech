package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.model.base.HubEvent;
import ru.yandex.practicum.model.base.SensorEvent;
import ru.yandex.practicum.service.KafkaEventProducer;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final KafkaEventProducer producer;

    @PostMapping("/sensors")
    @ResponseStatus(HttpStatus.OK)
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        producer.sendSensorEvent(event);
    }

    @PostMapping("/hubs")
    @ResponseStatus(HttpStatus.OK)
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        producer.sendHubEvent(event);
    }
}
