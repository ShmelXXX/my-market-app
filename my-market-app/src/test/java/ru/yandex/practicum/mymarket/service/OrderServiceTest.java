package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.client.PaymentWebClient;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.model.PaymentStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartService cartService;

    @Mock
    private PaymentWebClient paymentWebClient;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private final String TEST_SESSION_ID = "test-session-123";
    private final String TEST_USER_ID = "test-user-1";
    private Item testItem1;
    private Item testItem2;
    private CartItem cartItem1;
    private CartItem cartItem2;
    private PaymentResponse successPaymentResponse;

    @BeforeEach
    void setUp() {
        testItem1 = new Item();
        testItem1.setId(1L);
        testItem1.setTitle("Товар 1");
        testItem1.setPrice(1000L);
        testItem1.setStock(10);

        testItem2 = new Item();
        testItem2.setId(2L);
        testItem2.setTitle("Товар 2");
        testItem2.setPrice(500L);
        testItem2.setStock(5);

        cartItem1 = new CartItem();
        cartItem1.setId(1L);
        cartItem1.setItem(testItem1);
        cartItem1.setQuantity(2);
        cartItem1.setSessionId(TEST_SESSION_ID);

        cartItem2 = new CartItem();
        cartItem2.setId(2L);
        cartItem2.setItem(testItem2);
        cartItem2.setQuantity(3);
        cartItem2.setSessionId(TEST_SESSION_ID);

        successPaymentResponse = new PaymentResponse();
        successPaymentResponse.setPaymentId(UUID.randomUUID());
        successPaymentResponse.setStatus(PaymentStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("Должен создать заказ из корзины с успешным платежом")
    void shouldCreateOrderFromCartWithSuccessfulPayment() {
        List<CartItem> cartItems = Arrays.asList(cartItem1, cartItem2);
        when(cartItemRepository.findBySessionId(TEST_SESSION_ID)).thenReturn(cartItems);

        // Создаем savedOrder с правильными значениями
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setOrderDate(LocalDateTime.now());
        savedOrder.setTotalSum(3500L); // ВАЖНО: устанавливаем totalSum

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        when(paymentWebClient.createPayment(any())).thenReturn(Mono.just(successPaymentResponse));

        Order order = orderService.createOrder(TEST_SESSION_ID, TEST_USER_ID);

        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getTotalSum()).isEqualTo(3500L);

        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();

        assertThat(capturedOrder.getItems()).hasSize(2);
        assertThat(capturedOrder.getTotalSum()).isEqualTo(3500L);
        assertThat(capturedOrder.getOrderDate()).isNotNull();

        verify(cartService).clearCart(TEST_SESSION_ID);
        verify(paymentWebClient).createPayment(any());
    }

    @Test
    @DisplayName("Должен получить все заказы")
    void shouldGetAllOrders() {
        Order order1 = new Order();
        order1.setId(1L);
        order1.setTotalSum(1000L);

        Order order2 = new Order();
        order2.setId(2L);
        order2.setTotalSum(2000L);

        List<Order> expectedOrders = Arrays.asList(order1, order2);

        when(orderRepository.findAll()).thenReturn(expectedOrders);

        List<Order> actualOrders = orderService.getAllOrders();

        assertThat(actualOrders).hasSize(2);
        verify(orderRepository).findAll();
    }

    @Test
    @DisplayName("Должен получить заказ по ID")
    void shouldGetOrderById() {
        Order order = new Order();
        order.setId(1L);
        order.setTotalSum(1000L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order foundOrder = orderService.getOrderById(1L);

        assertThat(foundOrder.getId()).isEqualTo(1L);
    }
}