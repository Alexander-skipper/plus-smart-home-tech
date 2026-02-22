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
public class MotionSensorEvent extends SensorEvent {

    @NotNull(message = "Качество сигнала не может быть null")
    private Integer linkQuality;

    @NotNull(message = "Состояние движения не может быть null")
    private Boolean motion;

    @NotNull(message = "Напряжение не может быть null")
    private Integer voltage;
}