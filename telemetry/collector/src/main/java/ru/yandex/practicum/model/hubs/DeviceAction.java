package ru.yandex.practicum.model.hubs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ru.yandex.practicum.model.enums.ActionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAction {

    @NotBlank(message = "ID датчика не может быть пустым")
    private String sensorId;

    @NotNull(message = "Тип действия не может быть null")
    private ActionType type;

    private Integer value;
}
