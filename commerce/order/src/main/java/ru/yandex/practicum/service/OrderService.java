package ru.yandex.practicum.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.*;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.entity.AddressEmbeddable;
import ru.yandex.practicum.entity.Order;
import ru.yandex.practicum.exceptions.NoOrderFoundException;
import ru.yandex.practicum.exceptions.NoProductsInShoppingCartException;
import ru.yandex.practicum.repository.OrderRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShoppingCartClient shoppingCartClient;
    private final WarehouseClient warehouseClient;
    private final PaymentClient paymentClient;
    private final DeliveryClient deliveryClient;


    @Autowired
    public OrderService(OrderRepository orderRepository,
                        ShoppingCartClient shoppingCartClient,
                        WarehouseClient warehouseClient,
                        PaymentClient paymentClient,
                        DeliveryClient deliveryClient) {
        this.orderRepository = orderRepository;
        this.shoppingCartClient = shoppingCartClient;
        this.warehouseClient = warehouseClient;
        this.paymentClient = paymentClient;
        this.deliveryClient = deliveryClient;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getClientOrders(String username) {
        return orderRepository.findByUsername(username).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId) {
        Order order = getOrderOrThrow(orderId);
        return convertToDto(order);
    }

    @Transactional
    public OrderDto createNewOrder(CreateNewOrderRequest request) {
        String username = request.getUsername();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username не может быть пустым");
        }

        ShoppingCartDto cart = request.getShoppingCart();

        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            throw new NoProductsInShoppingCartException("Корзина пуста");
        }

        BookedProductsDto booked = warehouseClient.checkProductQuantityEnoughForShoppingCart(cart);

        Order order = Order.builder()
                .shoppingCartId(cart.getShoppingCartId())
                .products(cart.getProducts())
                .state(OrderState.NEW)
                .deliveryWeight(booked.getDeliveryWeight())
                .deliveryVolume(booked.getDeliveryVolume())
                .fragile(booked.getFragile())
                .deliveryAddress(convertToEmbeddable(request.getDeliveryAddress()))
                .username(username)
                .build();

        Order savedOrder = orderRepository.save(order);

        shoppingCartClient.deactivateCurrentShoppingCart(username);

        return convertToDto(savedOrder);
    }

    @Transactional
    public OrderDto assembly(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getState() != OrderState.NEW) {
            throw new IllegalStateException("Заказ должен быть в статусе NEW для сборки");
        }

        AssemblyProductsForOrderRequest assemblyRequest = new AssemblyProductsForOrderRequest(
                orderId, order.getProducts());
        BookedProductsDto booked = warehouseClient.assemblyProductsForOrder(assemblyRequest);

        order.setState(OrderState.ASSEMBLED);
        order.setDeliveryWeight(booked.getDeliveryWeight());
        order.setDeliveryVolume(booked.getDeliveryVolume());
        order.setFragile(booked.getFragile());

        Order savedOrder = orderRepository.save(order);
        return convertToDto(savedOrder);
    }

    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        Order order = getOrderOrThrow(orderId);
        order.setState(OrderState.ASSEMBLY_FAILED);
        return convertToDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto calculateDeliveryCost(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        Double cost = deliveryClient.deliveryCost(orderId);

        order.setDeliveryPrice(cost);
        return convertToDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto calculateTotalCost(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        OrderDto orderDto = convertToDto(order);
        Double totalCost = paymentClient.getTotalCost(orderDto);

        order.setTotalPrice(totalCost);
        return convertToDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto payment(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getState() != OrderState.ASSEMBLED) {
            throw new IllegalStateException("Заказ должен быть собран для оплаты");
        }

        OrderDto orderDto = convertToDto(order);
        PaymentDto payment = paymentClient.payment(orderDto);

        order.setPaymentId(payment.getPaymentId());
        order.setState(OrderState.ON_PAYMENT);
        order.setTotalPrice(payment.getTotalPayment());
        order.setDeliveryPrice(payment.getDeliveryTotal());
        order.setProductPrice(payment.getTotalPayment() - payment.getDeliveryTotal() - payment.getFeeTotal());

        return convertToDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        Order order = getOrderOrThrow(orderId);
        order.setState(OrderState.PAYMENT_FAILED);
        return convertToDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto delivery(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getState() != OrderState.PAID) {
            throw new IllegalStateException("Заказ должен быть оплачен для доставки");
        }

        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();
        DeliveryDto deliveryRequest = DeliveryDto.builder()
                .fromAddress(warehouseAddress)
                .toAddress(convertToAddressDto(order.getDeliveryAddress()))
                .orderId(orderId)
                .deliveryState(DeliveryState.CREATED)
                .build();

        DeliveryDto delivery = deliveryClient.planDelivery(deliveryRequest);
        order.setDeliveryId(delivery.getDeliveryId());
        order.setState(OrderState.ON_DELIVERY);

        ShippedToDeliveryRequest shippedRequest = new ShippedToDeliveryRequest(orderId, delivery.getDeliveryId());
        warehouseClient.shippedToDelivery(shippedRequest);

        return convertToDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        Order order = getOrderOrThrow(orderId);
        order.setState(OrderState.DELIVERY_FAILED);
        return convertToDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto complete(UUID orderId) {
        Order order = getOrderOrThrow(orderId);
        order.setState(OrderState.COMPLETED);
        return convertToDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        Order order = getOrderOrThrow(request.getOrderId());

        warehouseClient.acceptReturn(request.getProducts());

        order.setState(OrderState.PRODUCT_RETURNED);

        request.getProducts().forEach((productId, quantity) -> {
            Long currentQuantity = order.getProducts().get(productId);
            if (currentQuantity != null) {
                Long newQuantity = currentQuantity - quantity;
                if (newQuantity <= 0) {
                    order.getProducts().remove(productId);
                } else {
                    order.getProducts().put(productId, newQuantity);
                }
            }
        });

        return convertToDto(orderRepository.save(order));
    }

    private Order getOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Заказ не найден: " + orderId));
    }

    private AddressEmbeddable convertToEmbeddable(AddressDto dto) {
        if (dto == null) return null;
        return AddressEmbeddable.builder()
                .country(dto.getCountry())
                .city(dto.getCity())
                .street(dto.getStreet())
                .house(dto.getHouse())
                .flat(dto.getFlat())
                .build();
    }

    private AddressDto convertToAddressDto(AddressEmbeddable embeddable) {
        if (embeddable == null) return null;
        return new AddressDto(
                embeddable.getCountry(),
                embeddable.getCity(),
                embeddable.getStreet(),
                embeddable.getHouse(),
                embeddable.getFlat()
        );
    }

    private OrderDto convertToDto(Order order) {
        return OrderDto.builder()
                .orderId(order.getOrderId())
                .shoppingCartId(order.getShoppingCartId())
                .products(order.getProducts())
                .paymentId(order.getPaymentId())
                .deliveryId(order.getDeliveryId())
                .state(order.getState())
                .deliveryWeight(order.getDeliveryWeight())
                .deliveryVolume(order.getDeliveryVolume())
                .fragile(order.getFragile())
                .totalPrice(order.getTotalPrice())
                .deliveryPrice(order.getDeliveryPrice())
                .productPrice(order.getProductPrice())
                .build();
    }
}