package ru.yandex.practicum.model;

import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioConditionId implements Serializable {
    private Long scenario;
    private String sensor;
    private Long condition;
}