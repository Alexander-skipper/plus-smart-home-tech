package ru.yandex.practicum.model.hubs;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ru.yandex.practicum.model.base.HubEvent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioRemovedEvent extends HubEvent {

    @NotBlank(message = "Название сценария не может быть пустым")
    private String name;
}