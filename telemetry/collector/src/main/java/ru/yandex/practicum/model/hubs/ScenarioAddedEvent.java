package ru.yandex.practicum.model.hubs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ru.yandex.practicum.model.base.HubEvent;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioAddedEvent extends HubEvent {

    @NotBlank(message = "Название сценария не может быть пустым")
    private String name;

    @NotNull(message = "Список условий не может быть null")
    @NotEmpty(message = "Список условий не может быть пустым")
    private List<@NotNull(message = "Условие не может быть null") ScenarioCondition> conditions;

    @NotNull(message = "Список действий не может быть null")
    @NotEmpty(message = "Список действий не может быть пустым")
    private List<@NotNull(message = "Действие не может быть null") DeviceAction> actions;
}