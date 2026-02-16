package ru.yandex.practicum.model.hubs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ru.yandex.practicum.model.base.HubEvent;
import ru.yandex.practicum.model.enums.DeviceType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAddedEvent extends HubEvent {

    @NotBlank(message = "ID устройства не может быть пустым")
    private String id;

    @NotNull(message = "Тип устройства не может быть null")
    private DeviceType deviceType;
}