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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Юнит-тесты для CartService")
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private CartService cartService;

    @Captor
    private ArgumentCaptor<CartItem> cartItemCaptor;

    private final String TEST_SESSION_ID = "test-session-123";
    private final Long TEST_ITEM_ID = 1L;
    private Item testItem;
    private CartItem testCartItem;
    private User testUser;

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

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // Очищаем SecurityContext перед каждым тестом
        SecurityContextHolder.clearContext();

    }

    @Test
    @DisplayName("Должен получить список товаров корзины для авторизованного пользователя")
    void shouldGetCartItemsForAuthenticatedUser() {

        setupAuthenticatedUser();

        when(cartItemRepository.findByUser(testUser)).thenReturn(Collections.singletonList(testCartItem));

        List<CartItem> actualItems = cartService.getCartItems(TEST_SESSION_ID);

        assertThat(actualItems).hasSize(1);
        assertThat(actualItems.getFirst().getItem().getTitle()).isEqualTo("Тестовый товар");

        verify(cartItemRepository).findByUser(testUser);
        verify(cartItemRepository, never()).findBySessionId(anyString());
        verify(userRepository).findByUsername(testUser.getUsername());
    }

    @Test
    @DisplayName("Должен посчитать общую сумму корзины для авторизованного пользователя")
    void shouldGetTotalSumForAuthenticatedUser() {

        setupAuthenticatedUser();

        List<CartItem> cartItems = Collections.singletonList(testCartItem);
        when(cartItemRepository.findByUser(testUser)).thenReturn(Collections.singletonList(testCartItem));

        Long totalSum = cartService.getTotalSum(TEST_SESSION_ID);

        assertThat(totalSum).isEqualTo(2000L); // 2 * 1000
    }

    @Test
    @DisplayName("Должен добавить новый товар в корзину при PLUS для авторизованного пользователя")
    void shouldAddNewItemToCartForAuthenticatedUser() {

        setupAuthenticatedUser();

        when(cartItemRepository.findByItemIdAndUser(TEST_ITEM_ID, testUser))
                .thenReturn(Optional.empty());
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.of(testItem));

        cartService.updateCartItem(TEST_ITEM_ID, "PLUS", TEST_SESSION_ID);

        verify(cartItemRepository).save(cartItemCaptor.capture());
        CartItem savedCartItem = cartItemCaptor.getValue();

        assertThat(savedCartItem.getItem().getId()).isEqualTo(TEST_ITEM_ID);
        assertThat(savedCartItem.getQuantity()).isEqualTo(1);
        assertThat(savedCartItem.getUser()).isEqualTo(testUser);
        assertThat(savedCartItem.getSessionId()).isEqualTo(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("Должен уменьшить количество товара при MINUS для авторизованного пользователя")
    void shouldDecreaseItemQuantityWhenMinus() {

        setupAuthenticatedUser();

        testCartItem.setUser(testUser);

        when(cartItemRepository.findByItemIdAndUser(TEST_ITEM_ID, testUser))
                .thenReturn(Optional.of(testCartItem));
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.of(testItem));

        cartService.updateCartItem(TEST_ITEM_ID, "MINUS", TEST_SESSION_ID);

        verify(cartItemRepository).save(testCartItem);
        assertThat(testCartItem.getQuantity()).isEqualTo(1); // 2 - 1
    }

    @Test
    @DisplayName("Должен удалить товар при DELETE для авторизованного пользователя")
    void shouldDeleteItemWhenDelete() {

        setupAuthenticatedUser();
        testCartItem.setUser(testUser);

        when(cartItemRepository.findByItemIdAndUser(TEST_ITEM_ID, testUser))
                .thenReturn(Optional.of(testCartItem));
        when(itemRepository.findById(TEST_ITEM_ID)).thenReturn(Optional.of(testItem));

        cartService.updateCartItem(TEST_ITEM_ID, "DELETE", TEST_SESSION_ID);

        verify(cartItemRepository).delete(testCartItem);
        verify(itemRepository).findById(TEST_ITEM_ID); // Проверяем, что метод был вызван
    }

    @Test
    @DisplayName("Должен получить количество товара в корзине для авторизованного пользователя")
    void shouldGetItemCountInCart() {

        setupAuthenticatedUser();
        testCartItem.setUser(testUser);

        when(cartItemRepository.findByItemIdAndUser(TEST_ITEM_ID, testUser)).thenReturn(Optional.of(testCartItem));

        int count = cartService.getItemCountInCart(TEST_ITEM_ID, TEST_SESSION_ID);

        assertThat(count).isEqualTo(2);
        verify(cartItemRepository).findByItemIdAndUser(TEST_ITEM_ID, testUser);
    }


    @Test
    @DisplayName("Должен очистить корзину для авторизованного пользователя")
    void shouldClearCart() {

        setupAuthenticatedUser();

        doNothing().when(cartItemRepository).deleteByUser(testUser);

        cartService.clearCart(TEST_SESSION_ID);

        verify(cartItemRepository).deleteByUser(testUser);
        verify(cartItemRepository, never()).deleteBySessionId(anyString());
    }

    private void setupAuthenticatedUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authentication.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
    }
}