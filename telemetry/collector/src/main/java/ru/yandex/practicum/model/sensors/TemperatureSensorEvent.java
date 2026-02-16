package ru.yandex.practicum.model.sensors;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ru.yandex.practicum.model.base.SensorEvent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureSensorEvent extends SensorEvent {

    @NotNull(message = "Температура в градусах Цельсия не может быть null")
    private Integer temperatureC;

    @NotNull(message = "Температура в градусах Фаренгейта не может быть null")
    private Integer temperatureF;
}