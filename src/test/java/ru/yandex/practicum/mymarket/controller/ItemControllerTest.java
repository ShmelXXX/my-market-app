package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ui.Model;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Реактивные тесты для ItemController")
class ItemControllerTest {

    @Mock
    private ItemService itemService;

    @Mock
    private CartService cartService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @InjectMocks
    private ItemController itemController;

    private final String TEST_SESSION_ID = "test-session-123";
    private Item item1;
    private Item item2;
    private List<Item> items;

    @BeforeEach
    void setUp() {
        item1 = new Item();
        item1.setId(1L);
        item1.setTitle("Футбольный мяч");
        item1.setDescription("Качественный футбольный мяч");
        item1.setImgPath("images/soccer_ball.png");
        item1.setPrice(2500L);
        item1.setStock(20);

        item2 = new Item();
        item2.setId(2L);
        item2.setTitle("Бейсболка красная");
        item2.setDescription("Красная бейсболка");
        item2.setImgPath("images/baseball_cap_red.png");
        item2.setPrice(1000L);
        item2.setStock(5);

        items = Arrays.asList(item1, item2);
    }

    @Test
    @DisplayName("GET / - должен отобразить главную страницу с товарами")
    void shouldShowHomePage() {
        // Arrange
        int pageNumber = 1;
        int pageSize = 5;
        String search = null;
        String sort = "NO";

        lenient().when(request.getSession()).thenReturn(session);
        lenient().when(session.getId()).thenReturn(TEST_SESSION_ID);
        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
        when(itemService.getItems(eq(search), eq(sort), eq(pageNumber), eq(pageSize)))
                .thenReturn(Flux.fromIterable(items));
        when(itemService.getTotalItems(eq(search))).thenReturn(Mono.just(2L));
        when(cartService.getItemCountInCart(eq(1L), eq(TEST_SESSION_ID))).thenReturn(Mono.just(0));
        when(cartService.getItemCountInCart(eq(2L), eq(TEST_SESSION_ID))).thenReturn(Mono.just(0));

        // Act
        Mono<String> result = itemController.getItems(
                search, sort, pageNumber, pageSize, request, model);

        // Assert
        StepVerifier.create(result)
                .assertNext(viewName -> {
                    assertThat(viewName).isEqualTo("items");

                    verify(itemService).getItems(eq(search), eq(sort), eq(pageNumber), eq(pageSize));
                    verify(itemService).getTotalItems(eq(search));
                    verify(cartService, times(2)).getItemCountInCart(anyLong(), eq(TEST_SESSION_ID));
                    verify(model).addAttribute(eq("items"), any(List.class));
                    verify(model).addAttribute(eq("search"), eq(search));
                    verify(model).addAttribute(eq("sort"), eq(sort));
                    verify(model).addAttribute(eq("paging"), any());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GET /items - должен отобразить страницу товаров с поиском")
    void shouldShowItemsPageWithSearch() {
        // Arrange
        String searchQuery = "мяч";
        String sort = "NO";
        int pageNumber = 1;
        int pageSize = 5;
        List<Item> searchResults = Arrays.asList(item1);

        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
        when(itemService.getItems(eq(searchQuery), eq(sort), eq(pageNumber), eq(pageSize)))
                .thenReturn(Flux.fromIterable(searchResults));
        when(itemService.getTotalItems(eq(searchQuery))).thenReturn(Mono.just(1L));
        when(cartService.getItemCountInCart(eq(1L), eq(TEST_SESSION_ID))).thenReturn(Mono.just(2));

        // Act
        Mono<String> result = itemController.getItems(
                searchQuery, sort, pageNumber, pageSize, request, model);

        // Assert
        StepVerifier.create(result)
                .assertNext(viewName -> {
                    assertThat(viewName).isEqualTo("items");

                    verify(itemService).getItems(eq(searchQuery), eq(sort), eq(pageNumber), eq(pageSize));
                    verify(itemService).getTotalItems(eq(searchQuery));
                    verify(cartService).getItemCountInCart(eq(1L), eq(TEST_SESSION_ID));
                    verify(model).addAttribute(eq("search"), eq(searchQuery));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GET /items/{id} - должен отобразить страницу товара")
    void shouldShowItemPage() {
        // Arrange
        Long itemId = 1L;

        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
        when(itemService.getItemById(itemId)).thenReturn(Mono.just(item1));
        when(cartService.getItemCountInCart(itemId, TEST_SESSION_ID)).thenReturn(Mono.just(3));

        // Act
        Mono<String> result = itemController.getItem(itemId, request, model);

        // Assert
        StepVerifier.create(result)
                .assertNext(viewName -> {
                    assertThat(viewName).isEqualTo("item");

                    verify(itemService).getItemById(itemId);
                    verify(cartService).getItemCountInCart(itemId, TEST_SESSION_ID);
                    verify(model).addAttribute(eq("item"), any(ItemDto.class));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GET /items/{id} - должен вернуть ошибку при отсутствии товара")
    void shouldReturnErrorWhenItemNotFound() {
        // Arrange
        Long itemId = 999L;

        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
        when(itemService.getItemById(itemId)).thenReturn(Mono.empty());

        // Act
        Mono<String> result = itemController.getItem(itemId, request, model);

        // Assert
        StepVerifier.create(result)
                .assertNext(viewName -> {
                    assertThat(viewName).isEqualTo("error");
                    verify(model).addAttribute(eq("error"), eq("Товар не найден"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("POST /items - должен добавить товар в корзину и перенаправить")
    void shouldAddItemToCartAndRedirect() {
        // Arrange
        Long itemId = 1L;
        String action = "PLUS";
        String sort = "NO";
        int pageNumber = 1;
        int pageSize = 5;
        String search = null;

        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
        when(cartService.updateCartItem(eq(itemId), eq(action), eq(TEST_SESSION_ID)))
                .thenReturn(Mono.empty());

        // Act
        Mono<String> result = itemController.updateCartItemFromItems(
                itemId, action, search, sort, pageNumber, pageSize, request);

        // Assert
        StepVerifier.create(result)
                .assertNext(viewName -> {
                    assertThat(viewName).isEqualTo("redirect:/items?id=1&sort=NO&pageNumber=1&pageSize=5");
                    verify(cartService).updateCartItem(eq(itemId), eq(action), eq(TEST_SESSION_ID));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("POST /items/{id} - должен обновить корзину со страницы товара")
    void shouldUpdateCartFromItemPage() {
        // Arrange
        Long itemId = 1L;
        String action = "PLUS";

        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
        when(cartService.updateCartItem(eq(itemId), eq(action), eq(TEST_SESSION_ID)))
                .thenReturn(Mono.empty());
        when(itemService.getItemById(itemId)).thenReturn(Mono.just(item1));
        when(cartService.getItemCountInCart(itemId, TEST_SESSION_ID)).thenReturn(Mono.just(1));

        // Act
        Mono<String> result = itemController.updateCartItemFromItem(itemId, action, request, model);

        // Assert
        StepVerifier.create(result)
                .assertNext(viewName -> {
                    assertThat(viewName).isEqualTo("item");
                    verify(cartService).updateCartItem(eq(itemId), eq(action), eq(TEST_SESSION_ID));
                    verify(itemService).getItemById(itemId);
                    verify(cartService).getItemCountInCart(itemId, TEST_SESSION_ID);
                    verify(model).addAttribute(eq("item"), any(ItemDto.class));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GET /items - должен обработать разные значения сортировки")
    void shouldHandleDifferentSortValues() {
        // Arrange
        String search = null;
        String sort = "PRICE";
        int pageNumber = 1;
        int pageSize = 5;

        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
        when(itemService.getItems(eq(search), eq(sort), eq(pageNumber), eq(pageSize)))
                .thenReturn(Flux.empty());
        when(itemService.getTotalItems(eq(search))).thenReturn(Mono.just(0L));

        // Act
        Mono<String> result = itemController.getItems(
                search, sort, pageNumber, pageSize, request, model);

        // Assert
        StepVerifier.create(result)
                .assertNext(viewName -> {
                    assertThat(viewName).isEqualTo("items");
                    verify(itemService).getItems(eq(search), eq(sort), eq(pageNumber), eq(pageSize));
                    verify(itemService).getTotalItems(eq(search));
                    verify(model).addAttribute(eq("sort"), eq(sort));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GET /items - должен корректно обрабатывать параметры пагинации")
    void shouldHandlePaginationParameters() {
        // Arrange
        String search = null;
        String sort = "NO";
        int pageNumber = 3;
        int pageSize = 10;

        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
        when(itemService.getItems(eq(search), eq(sort), eq(pageNumber), eq(pageSize)))
                .thenReturn(Flux.empty());
        when(itemService.getTotalItems(eq(search))).thenReturn(Mono.just(0L));

        // Act
        Mono<String> result = itemController.getItems(
                search, sort, pageNumber, pageSize, request, model);

        // Assert
        StepVerifier.create(result)
                .assertNext(viewName -> {
                    assertThat(viewName).isEqualTo("items");

                    verify(itemService).getItems(eq(search), eq(sort), eq(pageNumber), eq(pageSize));
                    verify(itemService).getTotalItems(eq(search));

                    verify(model).addAttribute(eq("paging"), argThat(paging -> {
                        // Используем рефлексию для доступа к record компонентам
                        try {
                            var pageNumberField = paging.getClass().getDeclaredMethod("pageNumber");
                            var pageSizeField = paging.getClass().getDeclaredMethod("pageSize");

                            int actualPageNumber = (int) pageNumberField.invoke(paging);
                            int actualPageSize = (int) pageSizeField.invoke(paging);

                            assertThat(actualPageNumber).isEqualTo(pageNumber);
                            assertThat(actualPageSize).isEqualTo(pageSize);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return true;
                    }));
                })
                .verifyComplete();
    }
}