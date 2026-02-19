package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;

    @Transactional
    public Order createOrder(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Некорректный ID сессии");
        }

        List<CartItem> cartItems = cartItemRepository.findBySessionId(sessionId);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Корзина пуста");
        }

        // Проверка наличия всех товаров на складе
        for (CartItem cartItem : cartItems) {
            Item item = cartItem.getItem();
            if (item.getStock() < cartItem.getQuantity()) {
                throw new IllegalStateException(
                        "Товар '" + item.getTitle() + "' недоступен в нужном количестве. " +
                                "Доступно: " + item.getStock() + ", запрошено: " + cartItem.getQuantity()
                );
            }
        }

        Order order = new Order();
        order.setOrderDate(LocalDateTime.now());

        long totalSum = 0;

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setItem(cartItem.getItem());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getItem().getPrice());

            order.getItems().add(orderItem);
            totalSum += cartItem.getItem().getPrice() * cartItem.getQuantity();

            Item item = cartItem.getItem();
            item.setStock(item.getStock() - cartItem.getQuantity());
        }

        order.setTotalSum(totalSum);

        Order savedOrder = orderRepository.save(order);

        // Очищаем корзину после создания заказа
        cartService.clearCart(sessionId);

        return savedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }
}