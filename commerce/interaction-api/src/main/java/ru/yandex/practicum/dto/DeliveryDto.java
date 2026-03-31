package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryDto {

    @NotNull(message = "ID доставки обязателен")
    private UUID deliveryId;

    @NotNull(message = "Адрес отправителя обязателен")
    private AddressDto fromAddress;

    @NotNull(message = "Адрес получателя обязателен")
    private AddressDto toAddress;

    @NotNull(message = "ID заказа обязателен")
    private UUID orderId;

    @NotNull(message = "Статус доставки обязателен")
    private DeliveryState deliveryState;
}