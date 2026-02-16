package ru.yandex.practicum.model.base;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.yandex.practicum.model.enums.SensorEventType;
import ru.yandex.practicum.model.sensors.*;

import java.time.Instant;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LightSensorEvent.class, name = "LIGHT_SENSOR_EVENT"),
        @JsonSubTypes.Type(value = ClimateSensorEvent.class, name = "CLIMATE_SENSOR_EVENT"),
        @JsonSubTypes.Type(value = MotionSensorEvent.class, name = "MOTION_SENSOR_EVENT"),
        @JsonSubTypes.Type(value = SwitchSensorEvent.class, name = "SWITCH_SENSOR_EVENT"),
        @JsonSubTypes.Type(value = TemperatureSensorEvent.class, name = "TEMPERATURE_SENSOR_EVENT")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class SensorEvent {

    @NotBlank(message = "ID события не может быть пустым")
    private String id;

    @NotBlank(message = "ID хаба не может быть пустым")
    private String hubId;

    @NotNull(message = "Временная метка не может быть null")
    private Instant timestamp = Instant.now();

    @NotNull(message = "Тип события не может быть null")
    private SensorEventType type;
}