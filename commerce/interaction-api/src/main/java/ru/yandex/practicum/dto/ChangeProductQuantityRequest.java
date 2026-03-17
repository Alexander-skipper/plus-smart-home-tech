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
public class ChangeProductQuantityRequest {

    @NotNull(message = "ID продукта обязательно")
    private UUID productId;

    @NotNull(message = "Новое количество обязательно")
    @Min(value = 1, message = "Количество должно быть не меньше 1")
    private Long newQuantity;
}