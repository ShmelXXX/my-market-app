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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private final String TEST_SESSION_ID = "test-session-123";
    private CartService.CartItemWithItem cartItemWithItem1;
    private CartService.CartItemWithItem cartItemWithItem2;

    @BeforeEach
    void setUp() {
        Item testItem1 = new Item();
        testItem1.setId(1L);
        testItem1.setTitle("Товар 1");
        testItem1.setPrice(1000L);
        testItem1.setStock(10);

        Item testItem2 = new Item();
        testItem2.setId(2L);
        testItem2.setTitle("Товар 2");
        testItem2.setPrice(500L);
        testItem2.setStock(5);

        CartItem cartItem1 = new CartItem();
        cartItem1.setId(1L);
        cartItem1.setItem(testItem1);
        cartItem1.setQuantity(2);
        cartItem1.setSessionId(TEST_SESSION_ID);

        CartItem cartItem2 = new CartItem();
        cartItem2.setId(2L);
        cartItem2.setItem(testItem2);
        cartItem2.setQuantity(3);
        cartItem2.setSessionId(TEST_SESSION_ID);

        cartItemWithItem1 = new CartService.CartItemWithItem(cartItem1, testItem1);
        cartItemWithItem2 = new CartService.CartItemWithItem(cartItem2, testItem2);
    }

    @Test
    @DisplayName("Должен создать заказ из корзины")
    void shouldCreateOrderFromCart() {
        // Arrange
        List<CartService.CartItemWithItem> cartItems = Arrays.asList(cartItemWithItem1, cartItemWithItem2);

        when(cartService.getCartItemsWithDetails(TEST_SESSION_ID))
                .thenReturn(Flux.fromIterable(cartItems));

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setOrderDate(LocalDateTime.now());
        savedOrder.setTotalSum(3500L);

        when(orderRepository.save(any(Order.class)))
                .thenReturn(Mono.just(savedOrder));

        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(cartService.clearCart(TEST_SESSION_ID))
                .thenReturn(Mono.empty());

        // Act
        Mono<Order> result = orderService.createOrder(TEST_SESSION_ID);

        // Assert
        StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getId()).isEqualTo(1L);
                    assertThat(order.getTotalSum()).isEqualTo(3500L);
                })
                .verifyComplete();

        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();

        assertThat(capturedOrder.getTotalSum()).isEqualTo(3500L);
        assertThat(capturedOrder.getOrderDate()).isNotNull();

        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
        verify(cartService).clearCart(TEST_SESSION_ID);
    }


    @Test
    @DisplayName("Должен получить все заказы")
    void shouldGetAllOrders() {
        // Arrange
        Order order1 = new Order();
        order1.setId(1L);
        order1.setOrderDate(LocalDateTime.now());
        order1.setTotalSum(1000L);

        Order order2 = new Order();
        order2.setId(2L);
        order2.setOrderDate(LocalDateTime.now());
        order2.setTotalSum(2000L);

        when(orderRepository.findAll()).thenReturn(Flux.just(order1, order2));

        // Act
        Flux<Order> result = orderService.getAllOrders();

        // Assert
        StepVerifier.create(result)
                .expectNext(order1)
                .expectNext(order2)
                .verifyComplete();

        verify(orderRepository).findAll();
    }

    @Test
    @DisplayName("Должен получить заказ по ID")
    void shouldGetOrderById() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalSum(1000L);

        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));

        // Act
        Mono<Order> result = orderService.getOrderById(1L);

        // Assert
        StepVerifier.create(result)
                .assertNext(foundOrder -> {
                    assertThat(foundOrder.getId()).isEqualTo(1L);
                    assertThat(foundOrder.getTotalSum()).isEqualTo(1000L);
                })
                .verifyComplete();

        verify(orderRepository).findById(1L);
    }
}