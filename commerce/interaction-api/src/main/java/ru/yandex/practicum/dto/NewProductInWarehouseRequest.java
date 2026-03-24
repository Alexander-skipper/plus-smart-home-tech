package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewProductInWarehouseRequest {

    @NotNull(message = "ID продукта обязательно")
    private UUID productId;

    @NotNull(message = "Информация о хрупкости обязательна")
    private Boolean fragile;

    @NotNull(message = "Размеры обязательны")
    private DimensionDto dimension;

    @NotNull(message = "Вес обязателен")
    @Min(value = 1, message = "Вес должен быть не меньше 1")
    private Double weight;
}