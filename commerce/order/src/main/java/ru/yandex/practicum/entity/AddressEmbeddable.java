package ru.yandex.practicum.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressEmbeddable {

    private String country;
    private String city;
    private String street;
    private String house;
    private String flat;
}