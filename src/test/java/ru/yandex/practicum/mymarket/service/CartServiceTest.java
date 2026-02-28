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
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Реактивные юнит-тесты для CartService")
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private CartService cartService;

    @Captor
    private ArgumentCaptor<CartItem> cartItemCaptor;

    private final String TEST_SESSION_ID = "test-session-123";
    private final Long TEST_ITEM_ID = 1L;
    private Item testItem;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testItem = new Item();
        testItem.setId(TEST_ITEM_ID);
        testItem.setTitle("Тестовый товар");
        testItem.setImgPath("images/test.png");
        testItem.setPrice(1000L);
        testItem.setStock(10);

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setItemId(TEST_ITEM_ID);
        testCartItem.setQuantity(2);
        testCartItem.setSessionId(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("Должен получить список товаров корзины")
    void shouldGetCartItems() {
        // Arrange
        when(cartItemRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Flux.just(testCartItem));

        // Act
        Flux<CartItem> result = cartService.getCartItems(TEST_SESSION_ID);

        // Assert
        StepVerifier.create(result)
                .assertNext(cartItem -> {
                    assertThat(cartItem.getItemId()).isEqualTo(TEST_ITEM_ID);
                    assertThat(cartItem.getQuantity()).isEqualTo(2);
                    assertThat(cartItem.getSessionId()).isEqualTo(TEST_SESSION_ID);
                })
                .verifyComplete();

        verify(cartItemRepository).findBySessionId(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("Должен посчитать общую сумму корзины")
    void shouldGetTotalSum() {
        // Arrange
        when(cartItemRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Flux.just(testCartItem));
        when(itemRepository.findById(TEST_ITEM_ID))
                .thenReturn(Mono.just(testItem));

        // Act
        Mono<Long> totalSum = cartService.getTotalSum(TEST_SESSION_ID);

        // Assert
        StepVerifier.create(totalSum)
                .expectNext(2000L) // 2 * 1000
                .verifyComplete();

        verify(cartItemRepository).findBySessionId(TEST_SESSION_ID);
        verify(itemRepository).findById(TEST_ITEM_ID);
    }

    @Test
    @DisplayName("Должен добавить новый товар в корзину при PLUS")
    void shouldAddNewItemToCartWhenPlus() {
        // Arrange
        when(cartItemRepository.findBySessionIdAndItemId(TEST_SESSION_ID, TEST_ITEM_ID))
                .thenReturn(Mono.empty());
        when(itemRepository.findById(TEST_ITEM_ID))
                .thenReturn(Mono.just(testItem));
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<Void> result = cartService.updateCartItem(TEST_ITEM_ID, "PLUS", TEST_SESSION_ID);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartItemRepository).save(cartItemCaptor.capture());
        CartItem savedCartItem = cartItemCaptor.getValue();

        assertThat(savedCartItem.getItemId()).isEqualTo(TEST_ITEM_ID);
        assertThat(savedCartItem.getQuantity()).isEqualTo(1);
        assertThat(savedCartItem.getSessionId()).isEqualTo(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("Должен уменьшить количество товара при MINUS")
    void shouldDecreaseItemQuantityWhenMinus() {
        // Arrange
        testCartItem.setQuantity(2);
        when(cartItemRepository.findBySessionIdAndItemId(TEST_SESSION_ID, TEST_ITEM_ID))
                .thenReturn(Mono.just(testCartItem));
        when(itemRepository.findById(TEST_ITEM_ID))
                .thenReturn(Mono.just(testItem));
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<Void> result = cartService.updateCartItem(TEST_ITEM_ID, "MINUS", TEST_SESSION_ID);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartItemRepository).save(testCartItem);
        assertThat(testCartItem.getQuantity()).isEqualTo(1); // 2 - 1
    }

    @Test
    @DisplayName("Должен удалить товар при DELETE")
    void shouldDeleteItemWhenDelete() {
        // Arrange
        when(cartItemRepository.findBySessionIdAndItemId(TEST_SESSION_ID, TEST_ITEM_ID))
                .thenReturn(Mono.just(testCartItem));
        when(itemRepository.findById(TEST_ITEM_ID))
                .thenReturn(Mono.just(testItem));
        when(cartItemRepository.delete(any(CartItem.class)))
                .thenReturn(Mono.empty());

        // Act
        Mono<Void> result = cartService.updateCartItem(TEST_ITEM_ID, "DELETE", TEST_SESSION_ID);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartItemRepository).delete(testCartItem);
        verify(itemRepository).findById(TEST_ITEM_ID);
    }

    @Test
    @DisplayName("Должен получить количество товара в корзине")
    void shouldGetItemCountInCart() {
        // Arrange
        when(cartItemRepository.findBySessionIdAndItemId(TEST_SESSION_ID, TEST_ITEM_ID))
                .thenReturn(Mono.just(testCartItem));

        // Act
        Mono<Integer> count = cartService.getItemCountInCart(TEST_ITEM_ID, TEST_SESSION_ID);

        // Assert
        StepVerifier.create(count)
                .expectNext(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен очистить корзину")
    void shouldClearCart() {
        // Arrange
        when(cartItemRepository.deleteBySessionId(TEST_SESSION_ID))
                .thenReturn(Mono.empty());

        // Act
        Mono<Void> result = cartService.clearCart(TEST_SESSION_ID);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartItemRepository).deleteBySessionId(TEST_SESSION_ID);
    }
}