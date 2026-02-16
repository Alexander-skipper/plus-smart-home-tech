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
public class SwitchSensorEvent extends SensorEvent {

    @NotNull(message = "Состояние переключателя не может быть null")
    private Boolean state;
}
