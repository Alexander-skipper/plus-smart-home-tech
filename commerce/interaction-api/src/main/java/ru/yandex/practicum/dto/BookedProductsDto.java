package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookedProductsDto {

    @NotNull(message = "Вес доставки обязателен")
    private Double deliveryWeight;

    @NotNull(message = "Объем доставки обязателен")
    private Double deliveryVolume;

    @NotNull(message = "Флаг хрупкости обязателен")
    private Boolean fragile;


    public boolean requiresSpecialHandling() {
        return Boolean.TRUE.equals(fragile);
    }

    public boolean isWeightExceeds(double maxWeight) {
        return deliveryWeight != null && deliveryWeight > maxWeight;
    }

    public boolean isVolumeExceeds(double maxVolume) {
        return deliveryVolume != null && deliveryVolume > maxVolume;
    }
}