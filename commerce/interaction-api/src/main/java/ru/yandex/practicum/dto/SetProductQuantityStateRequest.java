package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetProductQuantityStateRequest {

    @NotNull(message = "ID продукта обязательно")
    private UUID productId;

    @NotNull(message = "Состояние количества обязательно")
    private QuantityState quantityState;
}