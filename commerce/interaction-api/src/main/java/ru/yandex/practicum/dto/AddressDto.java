package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    @NotBlank(message = "Страна обязательна для заполнения")
    @Size(min = 2, max = 50, message = "Название страны должно содержать от 2 до 50 символов")
    @Pattern(regexp = "^[а-яА-ЯёЁa-zA-Z\\s-]+$",
            message = "Название страны может содержать только буквы, пробелы и дефисы")
    private String country;

    @NotBlank(message = "Город обязателен для заполнения")
    @Size(min = 2, max = 100, message = "Название города должно содержать от 2 до 100 символов")
    @Pattern(regexp = "^[а-яА-ЯёЁa-zA-Z\\s-]+$",
            message = "Название города может содержать только буквы, пробелы и дефисы")
    private String city;

    @NotBlank(message = "Улица обязательна для заполнения")
    @Size(min = 3, max = 100, message = "Название улицы должно содержать от 3 до 100 символов")
    @Pattern(regexp = "^[а-яА-ЯёЁa-zA-Z0-9\\s-.]{3,100}$",
            message = "Название улицы может содержать буквы, цифры, пробелы, дефисы и точки")
    private String street;

    @NotBlank(message = "Номер дома обязателен для заполнения")
    @Pattern(regexp = "^[0-9]{1,5}([а-яА-ЯёЁa-zA-Z])?(\\/[0-9]{1,3})?$",
            message = "Номер дома должен быть в формате: 15, 15А, 15/2")
    private String house;

    @Pattern(regexp = "^$|^[0-9]{1,5}([а-яА-ЯёЁa-zA-Z])?$",
            message = "Номер квартиры должен быть в формате: 42, 42А (или пусто для частного дома)")
    private String flat;


    public boolean isValid() {
        return country != null && !country.trim().isEmpty() &&
                city != null && !city.trim().isEmpty() &&
                street != null && !street.trim().isEmpty() &&
                house != null && !house.trim().isEmpty();
    }
}