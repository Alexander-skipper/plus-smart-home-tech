package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.base.HubEvent;
import ru.yandex.practicum.model.enums.*;
import ru.yandex.practicum.model.hubs.*;
import java.util.stream.Collectors;

@Component
public class HubEventMapper {

    public HubEventAvro toAvro(HubEvent event) {

        if (event == null) {
            throw new IllegalArgumentException("HubEvent must not be null");
        }

        HubEventAvro avro = new HubEventAvro();
        avro.setHubId(event.getHubId());
        avro.setTimestamp(event.getTimestamp());

        HubEventType type = event.getType();

        switch (type) {

            case DEVICE_ADDED -> {
                DeviceAddedEvent e = (DeviceAddedEvent) event;

                DeviceAddedEventAvro payload = new DeviceAddedEventAvro();
                payload.setId(e.getId());
                payload.setType(DeviceTypeAvro.valueOf(e.getDeviceType().name()));

                avro.setPayload(payload);
            }

            case DEVICE_REMOVED -> {
                DeviceRemovedEvent e = (DeviceRemovedEvent) event;

                DeviceRemovedEventAvro payload = new DeviceRemovedEventAvro();
                payload.setId(e.getId());

                avro.setPayload(payload);
            }

            case SCENARIO_ADDED -> {
                ScenarioAddedEvent e = (ScenarioAddedEvent) event;

                ScenarioAddedEventAvro payload = new ScenarioAddedEventAvro();
                payload.setName(e.getName());

                payload.setConditions(
                        e.getConditions()
                                .stream()
                                .map(this::mapCondition)
                                .collect(Collectors.toList())
                );

                payload.setActions(
                        e.getActions()
                                .stream()
                                .map(this::mapAction)
                                .collect(Collectors.toList())
                );

                avro.setPayload(payload);
            }

            case SCENARIO_REMOVED -> {
                ScenarioRemovedEvent e = (ScenarioRemovedEvent) event;

                ScenarioRemovedEventAvro payload = new ScenarioRemovedEventAvro();
                payload.setName(e.getName());

                avro.setPayload(payload);
            }

            default -> throw new IllegalStateException("Unsupported HubEvent type: " + type);
        }

        return avro;
    }

    private ScenarioConditionAvro mapCondition(ScenarioCondition condition) {

        ScenarioConditionAvro avro = new ScenarioConditionAvro();

        avro.setSensorId(condition.getSensorId());
        avro.setType(ConditionTypeAvro.valueOf(condition.getType().name()));
        avro.setOperation(ConditionOperationAvro.valueOf(condition.getOperation().name()));

        Object value = condition.getValue();

        if (value == null) {
            avro.setValue(null);
        } else if (value instanceof Integer) {
            avro.setValue(value);
        } else if (value instanceof Boolean) {
            avro.setValue(value);
        } else {
            throw new IllegalArgumentException("Unsupported condition value type: " + value.getClass());
        }

        return avro;
    }

    private DeviceActionAvro mapAction(DeviceAction action) {

        DeviceActionAvro avro = new DeviceActionAvro();

        avro.setSensorId(action.getSensorId());
        avro.setType(ActionTypeAvro.valueOf(action.getType().name()));
        avro.setValue(action.getValue());

        return avro;
    }
}

