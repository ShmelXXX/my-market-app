package ru.yandex.practicum.mymarket.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для CartService")
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
        testItem.setPrice(1000L);
        testItem.setStock(10);

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setItem(testItem);
        testCartItem.setQuantity(2);
        testCartItem.setSessionId(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("Должен получить список товаров корзины")
    void shouldGetCartItems() {
        List<CartItem> expectedItems = Collections.singletonList(testCartItem);
        when(cartItemRepository.findBySessionId(TEST_SESSION_ID)).thenReturn(expectedItems);

        List<CartItem> actualItems = cartService.getCartItems(TEST_SESSION_ID);

        assertThat(actualItems).hasSize(1);
        assertThat(actualItems.getFirst().getItem().getTitle()).isEqualTo("Тестовый товар");
        verify(cartItemRepository).findBySessionId(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("Должен посчитать общую сумму корзины")
    void shouldGetTotalSum() {
        List<CartItem> cartItems = Collections.singletonList(testCartItem);
        when(cartItemRepository.findBySessionId(TEST_SESSION_ID)).thenReturn(cartItems);

        Long totalSum = cartService.getTotalSum(TEST_SESSION_ID);

        assertThat(totalSum).isEqualTo(2000L); // 2 * 1000
    }

    @Test
    @DisplayName("Должен добавить новый товар в корзину при PLUS")
    void shouldAddNewItemToCartWhenPlus() {
        when(cartItemRepository.findByItemIdAndSessionId(TEST_ITEM_ID, TEST_SESSION_ID))
                .thenReturn(Optional.empty());
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.of(testItem));

        cartService.updateCartItem(TEST_ITEM_ID, "PLUS", TEST_SESSION_ID);

        verify(cartItemRepository).save(cartItemCaptor.capture());
        CartItem savedCartItem = cartItemCaptor.getValue();

        assertThat(savedCartItem.getItem().getId()).isEqualTo(TEST_ITEM_ID);
        assertThat(savedCartItem.getQuantity()).isEqualTo(1);
        assertThat(savedCartItem.getSessionId()).isEqualTo(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("Должен уменьшить количество товара при MINUS")
    void shouldDecreaseItemQuantityWhenMinus() {
        when(cartItemRepository.findByItemIdAndSessionId(TEST_ITEM_ID, TEST_SESSION_ID))
                .thenReturn(Optional.of(testCartItem));
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.of(testItem));

        cartService.updateCartItem(TEST_ITEM_ID, "MINUS", TEST_SESSION_ID);

        verify(cartItemRepository).save(testCartItem);
        assertThat(testCartItem.getQuantity()).isEqualTo(1); // 2 - 1
    }

    @Test
    @DisplayName("Должен удалить товар при DELETE")
    void shouldDeleteItemWhenDelete() {
        when(cartItemRepository.findByItemIdAndSessionId(TEST_ITEM_ID, TEST_SESSION_ID))
                .thenReturn(Optional.of(testCartItem));
        // Для DELETE действия itemRepository.findById не вызывается, так как мы не обновляем товар
        // Но метод все равно вызывается в начале updateCartItem, поэтому нужно замокать
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.of(testItem));

        cartService.updateCartItem(TEST_ITEM_ID, "DELETE", TEST_SESSION_ID);

        verify(cartItemRepository).delete(testCartItem);
        verify(itemRepository).findById(TEST_ITEM_ID); // Проверяем, что метод был вызван
    }

    @Test
    @DisplayName("Должен получить количество товара в корзине")
    void shouldGetItemCountInCart() {
        when(cartItemRepository.findByItemIdAndSessionId(TEST_ITEM_ID, TEST_SESSION_ID))
                .thenReturn(Optional.of(testCartItem));

        int count = cartService.getItemCountInCart(TEST_ITEM_ID, TEST_SESSION_ID);

        assertThat(count).isEqualTo(2);
    }


    @Test
    @DisplayName("Должен очистить корзину")
    void shouldClearCart() {
        cartService.clearCart(TEST_SESSION_ID);

        verify(cartItemRepository).deleteBySessionId(TEST_SESSION_ID);
    }
}