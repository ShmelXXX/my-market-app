package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.client.PaymentWebClient;  // Заменили на WebClient
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.payment.model.BalanceResponse;  // Добавить импорт
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final PaymentWebClient paymentWebClient;

    @Transactional
    public Order createOrder(String sessionId, String userId) {
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

        // Создаем заказ
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

            // Уменьшаем количество товара на складе
            Item item = cartItem.getItem();
            item.setStock(item.getStock() - cartItem.getQuantity());
        }

        order.setTotalSum(totalSum);

        // Сохраняем заказ
        Order savedOrder = orderRepository.save(order);

        // Создаем платеж
        try {
            PaymentResponse paymentResponse = createPaymentForOrder(savedOrder, userId);
            log.info("Платеж создан: {}", paymentResponse.getPaymentId());

            // Очищаем корзину после успешного платежа
            cartService.clearCart(sessionId);

            return savedOrder;
        } catch (Exception e) {
            log.error("Ошибка при создании платежа для заказа {}, откатываем заказ", savedOrder.getId(), e);

            // Откатываем изменения в товарах
            rollbackStock(cartItems);

            // Удаляем заказ
            orderRepository.delete(savedOrder);

            throw new RuntimeException("Ошибка оплаты заказа: " + e.getMessage(), e);
        }
    }

    private void rollbackStock(List<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
            Item item = cartItem.getItem();
            item.setStock(item.getStock() + cartItem.getQuantity());

        }
    }

    private PaymentResponse createPaymentForOrder(Order order, String userId) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(order.getId().toString());
        paymentRequest.setAmount(order.getTotalSum().intValue() * 100);
        paymentRequest.setCurrency(PaymentRequest.CurrencyEnum.RUB);
        paymentRequest.setDescription("Оплата заказа №" + order.getId() + "|userId=" + userId);
        paymentRequest.setPaymentMethod(PaymentRequest.PaymentMethodEnum.CARD);

        try {
            paymentRequest.setReturnUrl(new URI("http://localhost:8080/orders/" + order.getId()));
        } catch (Exception e) {
            log.error("Ошибка создания URI", e);
            throw new RuntimeException("Неверный формат URL", e);
        }

        return paymentWebClient.createPayment(paymentRequest).block();
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    public boolean checkBalance(String userId, Long requiredAmount) {
        try {
            BalanceResponse balance = paymentWebClient.getUserBalance(userId).block();
            if (balance == null) {
                log.warn("Не удалось получить баланс для пользователя {}", userId);
                return false;
            }

            log.debug("Баланс пользователя {}: {} {}", userId, balance.getAmount(), balance.getCurrency());

            return BalanceResponse.StatusEnum.AVAILABLE.equals(balance.getStatus()) &&
                    balance.getAmount() >= requiredAmount;
        } catch (Exception e) {
            log.error("Ошибка при проверке баланса для пользователя {}: {}", userId, e.getMessage());
            return false;
        }
    }
}