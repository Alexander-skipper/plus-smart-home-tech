package ru.yandex.practicum.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.OrderClient;
import ru.yandex.practicum.client.ShoppingStoreClient;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.dto.PaymentState;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.entity.Payment;
import ru.yandex.practicum.exceptions.NotEnoughInfoInOrderToCalculateException;
import ru.yandex.practicum.exceptions.NoOrderFoundException;
import ru.yandex.practicum.repository.PaymentRepository;

import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final double VAT_RATE = 0.1; // 10% НДС

    private final PaymentRepository paymentRepository;
    private final ShoppingStoreClient shoppingStoreClient;
    private final OrderClient orderClient;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          ShoppingStoreClient shoppingStoreClient,
                          OrderClient orderClient) {
        this.paymentRepository = paymentRepository;
        this.shoppingStoreClient = shoppingStoreClient;
        this.orderClient = orderClient;
    }

    @Transactional(readOnly = true)
    public Double productCost(OrderDto orderDto) {
        validateOrder(orderDto);

        double total = 0.0;
        for (Map.Entry<UUID, Long> entry : orderDto.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            ProductDto product = shoppingStoreClient.getProduct(productId);
            total += product.getPrice() * quantity;
        }
        return total;
    }

    @Transactional(readOnly = true)
    public Double getTotalCost(OrderDto orderDto) {
        validateOrder(orderDto);

        double productCost = productCost(orderDto);
        double vat = productCost * VAT_RATE;
        double deliveryCost = orderDto.getDeliveryPrice() != null ? orderDto.getDeliveryPrice() : 0.0;

        return productCost + vat + deliveryCost;
    }

    @Transactional
    public PaymentDto payment(OrderDto orderDto) {
        validateOrder(orderDto);

        if (paymentRepository.findByOrderId(orderDto.getOrderId()).isPresent()) {
            throw new IllegalStateException("Оплата для заказа уже существует");
        }

        double productCost = productCost(orderDto);
        double vat = productCost * VAT_RATE;
        double deliveryCost = orderDto.getDeliveryPrice() != null ? orderDto.getDeliveryPrice() : 0.0;
        double totalPayment = productCost + vat + deliveryCost;

        Payment payment = Payment.builder()
                .orderId(orderDto.getOrderId())
                .totalPayment(totalPayment)
                .deliveryTotal(deliveryCost)
                .feeTotal(vat)
                .state(PaymentState.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        return PaymentDto.builder()
                .paymentId(savedPayment.getPaymentId())
                .totalPayment(savedPayment.getTotalPayment())
                .deliveryTotal(savedPayment.getDeliveryTotal())
                .feeTotal(savedPayment.getFeeTotal())
                .build();
    }

    @Transactional
    public void paymentSuccess(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoOrderFoundException("Платеж не найден: " + paymentId));

        payment.setState(PaymentState.SUCCESS);
        paymentRepository.save(payment);

        orderClient.payment(payment.getOrderId());
    }

    @Transactional
    public void paymentFailed(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoOrderFoundException("Платеж не найден: " + paymentId));

        payment.setState(PaymentState.FAILED);
        paymentRepository.save(payment);

        orderClient.paymentFailed(payment.getOrderId());
    }

    private void validateOrder(OrderDto orderDto) {
        if (orderDto == null) {
            throw new NotEnoughInfoInOrderToCalculateException("Заказ не может быть пустым");
        }
        if (orderDto.getProducts() == null || orderDto.getProducts().isEmpty()) {
            throw new NotEnoughInfoInOrderToCalculateException("В заказе нет товаров");
        }
    }
}