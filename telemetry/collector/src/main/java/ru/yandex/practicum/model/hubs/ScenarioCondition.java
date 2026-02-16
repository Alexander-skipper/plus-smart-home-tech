package ru.yandex.practicum.model.hubs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ru.yandex.practicum.model.enums.ConditionOperation;
import ru.yandex.practicum.model.enums.ConditionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioCondition {

    @NotBlank(message = "ID датчика не может быть пустым")
    private String sensorId;

    @NotNull(message = "Тип условия не может быть null")
    private ConditionType type;

    @NotNull(message = "Операция условия не может быть null")
    private ConditionOperation operation;

    private Object value;
}
