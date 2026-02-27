//package ru.yandex.practicum.mymarket.controller;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpSession;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.ui.Model;
//import ru.yandex.practicum.mymarket.dto.ItemDto;
//import ru.yandex.practicum.mymarket.dto.PagingDto;
//import ru.yandex.practicum.mymarket.model.Item;
//import ru.yandex.practicum.mymarket.service.CartService;
//import ru.yandex.practicum.mymarket.service.ItemService;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("Юнит-тесты для ItemController")
//class ItemControllerTest {
//
//    @Mock
//    private ItemService itemService;
//
//    @Mock
//    private CartService cartService;
//
//    @Mock
//    private HttpServletRequest request;
//
//    @Mock
//    private HttpSession session;
//
//    @Mock
//    private Model model;
//
//    @InjectMocks
//    private ItemController itemController;
//
//    private final String TEST_SESSION_ID = "test-session-123";
//    private Item item1;
//    private List<Item> items;
//
//    @BeforeEach
//    void setUp() {
//        item1 = new Item();
//        item1.setId(1L);
//        item1.setTitle("Футбольный мяч");
//        item1.setDescription("Качественный футбольный мяч");
//        item1.setImgPath("images/soccer_ball.png");
//        item1.setPrice(2500L);
//        item1.setStock(20);
//
//        Item item2 = new Item();
//        item2.setId(2L);
//        item2.setTitle("Бейсболка красная");
//        item2.setDescription("Красная бейсболка");
//        item2.setImgPath("images/baseball_cap_red.png");
//        item2.setPrice(1000L);
//        item2.setStock(5);
//
//        items = Arrays.asList(item1, item2);
//
//    }
//
//    @Test
//    @DisplayName("GET / - должен отобразить главную страницу с товарами")
//    void shouldShowHomePage() {
//        // Arrange
//        int pageNumber = 1;
//        int pageSize = 5;
//        Page<Item> itemPage = new PageImpl<>(items, PageRequest.of(0, pageSize), 2);
//
//        lenient().when(request.getSession()).thenReturn(session);
//        lenient().when(session.getId()).thenReturn(TEST_SESSION_ID);
//        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
//        when(itemService.getItems(isNull(), eq("NO"), eq(pageNumber), eq(pageSize)))
//                .thenReturn(itemPage);
//        when(cartService.getItemCountInCart(eq(1L), eq(TEST_SESSION_ID))).thenReturn(0);
//        when(cartService.getItemCountInCart(eq(2L), eq(TEST_SESSION_ID))).thenReturn(0);
//
//        // Act
//        String viewName = itemController.getItems(
//                null, "NO", pageNumber, pageSize, request, model);
//
//        // Assert
//        assertThat(viewName).isEqualTo("items");
//
//        verify(itemService).getItems(isNull(), eq("NO"), eq(pageNumber), eq(pageSize));
//        verify(cartService, times(2)).getItemCountInCart(anyLong(), eq(TEST_SESSION_ID));
//        verify(model).addAttribute(eq("items"), any(List.class));
//        verify(model).addAttribute(eq("search"), isNull());
//        verify(model).addAttribute(eq("sort"), eq("NO"));
//        verify(model).addAttribute(eq("paging"), any(PagingDto.class));
//    }
//
//    @Test
//    @DisplayName("GET /items - должен отобразить страницу товаров с поиском")
//    void shouldShowItemsPageWithSearch() {
//        // Arrange
//        String searchQuery = "мяч";
//        int pageNumber = 1;
//        int pageSize = 5;
//        List<Item> searchResults = Collections.singletonList(item1);
//        Page<Item> itemPage = new PageImpl<>(searchResults, PageRequest.of(0, pageSize), 1);
//
//        lenient().when(request.getSession()).thenReturn(session);
//        lenient().when(session.getId()).thenReturn(TEST_SESSION_ID);
//        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
//        when(itemService.getItems(eq(searchQuery), eq("NO"), eq(pageNumber), eq(pageSize)))
//                .thenReturn(itemPage);
//        when(cartService.getItemCountInCart(eq(1L), eq(TEST_SESSION_ID))).thenReturn(2);
//
//        // Act
//        String viewName = itemController.getItems(
//                searchQuery, "NO", pageNumber, pageSize, request, model);
//
//        // Assert
//        assertThat(viewName).isEqualTo("items");
//
//        verify(itemService).getItems(eq(searchQuery), eq("NO"), eq(pageNumber), eq(pageSize));
//        verify(cartService).getItemCountInCart(eq(1L), eq(TEST_SESSION_ID));
//        verify(model).addAttribute(eq("search"), eq(searchQuery));
//    }
//
//    @Test
//    @DisplayName("GET /items/{id} - должен отобразить страницу товара")
//    void shouldShowItemPage() {
//        // Arrange
//        Long itemId = 1L;
//
//        lenient().when(request.getSession()).thenReturn(session);
//        lenient().when(session.getId()).thenReturn(TEST_SESSION_ID);
//        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
//        when(itemService.getItemById(itemId)).thenReturn(item1);
//        when(cartService.getItemCountInCart(itemId, TEST_SESSION_ID)).thenReturn(3);
//
//        // Act
//        String viewName = itemController.getItem(itemId, request, model);
//
//        // Assert
//        assertThat(viewName).isEqualTo("item");
//
//        verify(itemService).getItemById(itemId);
//        verify(cartService).getItemCountInCart(itemId, TEST_SESSION_ID);
//        verify(model).addAttribute(eq("item"), any(ItemDto.class));
//    }
//
//    @Test
//    @DisplayName("POST /items - должен добавить товар в корзину и перенаправить")
//    void shouldAddItemToCartAndRedirect() {
//        // Arrange
//        Long itemId = 1L;
//        String action = "PLUS";
//        String sort = "NO";
//        int pageNumber = 1;
//        int pageSize = 5;
//        String search = null;
//
//        lenient().when(request.getSession()).thenReturn(session);
//        lenient().when(session.getId()).thenReturn(TEST_SESSION_ID);
//        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
//
//        // Act
//        String viewName = itemController.updateCartItemFromItems(
//                itemId, action, search, sort, pageNumber, pageSize, request);
//
//        // Assert
//        assertThat(viewName).isEqualTo("redirect:/items?id=1&sort=NO&pageNumber=1&pageSize=5");
//
//        verify(cartService).updateCartItem(eq(itemId), eq(action), eq(TEST_SESSION_ID));
//    }
//
//    @Test
//    @DisplayName("GET /items - должен обработать разные значения сортировки")
//    void shouldHandleDifferentSortValues() {
//        // Arrange
//        int pageNumber = 1;
//        int pageSize = 5;
//        Page<Item> emptyPage = Page.empty();
//
//        lenient().when(request.getSession()).thenReturn(session);
//        lenient().when(session.getId()).thenReturn(TEST_SESSION_ID);
//        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
//        when(itemService.getItems(isNull(), eq("PRICE"), eq(pageNumber), eq(pageSize)))
//                .thenReturn(emptyPage);
//
//        // Act
//        String viewName = itemController.getItems(
//                null, "PRICE", pageNumber, pageSize, request, model);
//
//        // Assert
//        assertThat(viewName).isEqualTo("items");
//
//        verify(itemService).getItems(isNull(), eq("PRICE"), eq(pageNumber), eq(pageSize));
//        verify(model).addAttribute(eq("sort"), eq("PRICE"));
//        verify(cartService, never()).getItemCountInCart(anyLong(), anyString());
//    }
//
//    @Test
//    @DisplayName("GET /items - должен корректно обрабатывать параметры пагинации")
//    void shouldHandlePaginationParameters() {
//        // Arrange
//        int pageNumber = 3;
//        int pageSize = 10;
//        Page<Item> emptyPage = Page.empty();
//
//        lenient().when(request.getSession()).thenReturn(session);
//        lenient().when(session.getId()).thenReturn(TEST_SESSION_ID);
//        when(cartService.getSessionId(request)).thenReturn(TEST_SESSION_ID);
//        when(itemService.getItems(isNull(), eq("NO"), eq(pageNumber), eq(pageSize)))
//                .thenReturn(emptyPage);
//
//        // Act
//        String viewName = itemController.getItems(
//                null, "NO", pageNumber, pageSize, request, model);
//
//        // Assert
//        assertThat(viewName).isEqualTo("items");
//
//        verify(itemService).getItems(isNull(), eq("NO"), eq(pageNumber), eq(pageSize));
//        verify(model).addAttribute(eq("paging"), argThat(paging -> {
//            PagingDto pagingDto = (PagingDto) paging;
//            assertThat(pagingDto.pageNumber()).isEqualTo(pageNumber);
//            assertThat(pagingDto.pageSize()).isEqualTo(pageSize);
//            return true;
//        }));
//    }
//}