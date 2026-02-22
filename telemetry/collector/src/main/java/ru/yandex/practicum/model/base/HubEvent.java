package ru.yandex.practicum.model.base;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.yandex.practicum.model.enums.HubEventType;
import ru.yandex.practicum.model.hubs.DeviceAddedEvent;
import ru.yandex.practicum.model.hubs.DeviceRemovedEvent;
import ru.yandex.practicum.model.hubs.ScenarioAddedEvent;
import ru.yandex.practicum.model.hubs.ScenarioRemovedEvent;

import java.time.Instant;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeviceAddedEvent.class, name = "DEVICE_ADDED"),
        @JsonSubTypes.Type(value = DeviceRemovedEvent.class, name = "DEVICE_REMOVED"),
        @JsonSubTypes.Type(value = ScenarioAddedEvent.class, name = "SCENARIO_ADDED"),
        @JsonSubTypes.Type(value = ScenarioRemovedEvent.class, name = "SCENARIO_REMOVED")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class HubEvent {

    @NotBlank(message = "ID хаба не может быть пустым")
    private String hubId;

    @NotNull(message = "Временная метка не может быть null")
    private Instant timestamp = Instant.now();

    @NotNull(message = "Тип события не может быть null")
    private HubEventType type;
}
