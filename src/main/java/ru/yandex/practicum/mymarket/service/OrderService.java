package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ItemService itemService;

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Mono<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Flux<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId)
                .flatMap(this::enrichOrderItemWithItemDetails);
    }

    private Mono<OrderItem> enrichOrderItemWithItemDetails(OrderItem orderItem) {
        return itemService.getItemById(orderItem.getItemId())
                .map(item -> {
                    orderItem.setItem(item);
                    return orderItem;
                })
                .defaultIfEmpty(orderItem);
    }

    public Mono<Order> createOrder(String sessionId) {
        return cartService.getCartItemsWithDetails(sessionId)
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new RuntimeException("Корзина пуста"));
                    }

                    Order order = new Order();
                    order.setOrderDate(LocalDateTime.now());

                    long totalSum = cartItems.stream()
                            .mapToLong(cartItem -> cartItem.getItem().getPrice() * cartItem.getQuantity())
                            .sum();
                    order.setTotalSum(totalSum);

                    return orderRepository.save(order)
                            .flatMap(savedOrder -> {
                                List<OrderItem> orderItems = cartItems.stream()
                                        .map(cartItem -> {
                                            OrderItem orderItem = new OrderItem();
                                            orderItem.setOrderId(savedOrder.getId());
                                            orderItem.setItemId(cartItem.getItemId());
                                            orderItem.setQuantity(cartItem.getQuantity());
                                            orderItem.setPrice(cartItem.getItem().getPrice());
                                            orderItem.setItem(cartItem.getItem());
                                            return orderItem;
                                        })
                                        .toList();

                                return Flux.fromIterable(orderItems)
                                        .flatMap(orderItemRepository::save)
                                        .then(Mono.just(savedOrder));
                            });
                })
                .flatMap(order ->
                        cartService.clearCart(sessionId)  // Очищаем корзину после создания заказа
                                .then(Mono.just(order))
                );
    }
}