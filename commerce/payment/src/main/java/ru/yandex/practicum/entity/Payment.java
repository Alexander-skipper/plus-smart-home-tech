package ru.yandex.practicum.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.dto.PaymentState;

import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payment_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentId;

    private UUID orderId;

    private Double totalPayment;

    private Double deliveryTotal;

    private Double feeTotal;

    @Enumerated(EnumType.STRING)
    private PaymentState state;

    @Version
    private Long version;
}