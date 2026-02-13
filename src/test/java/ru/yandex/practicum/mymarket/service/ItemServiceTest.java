// [file name]: service/ItemServiceTest.java (исправленная версия)
package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для ItemService")
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item testItem1;
    private Item testItem2;
    private Item testItem3;

    @BeforeEach
    void setUp() {
        testItem1 = new Item();
        testItem1.setId(1L);
        testItem1.setTitle("Футбольный мяч");
        testItem1.setDescription("Качественный мяч");
        testItem1.setPrice(2500L);
        testItem1.setStock(20);

        testItem2 = new Item();
        testItem2.setId(2L);
        testItem2.setTitle("Бейсболка красная");
        testItem2.setDescription("Красная бейсболка");
        testItem2.setPrice(1000L);
        testItem2.setStock(5);

        testItem3 = new Item();
        testItem3.setId(3L);
        testItem3.setTitle("Зонт");
        testItem3.setDescription("Супер зонт");
        testItem3.setPrice(1500L);
        testItem3.setStock(30);
    }

    @Test
    @DisplayName("Должен получить товары с пагинацией без сортировки и поиска")
    void shouldGetItemsWithPaginationNoSortNoSearch() {
        int pageNumber = 1;
        int pageSize = 2;
        List<Item> items = Arrays.asList(testItem1, testItem2);
        Page<Item> expectedPage = new PageImpl<>(items, PageRequest.of(0, pageSize), 3);

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);

        Page<Item> actualPage = itemService.getItems(null, "NO", pageNumber, pageSize);

        assertThat(actualPage.getContent()).hasSize(2);
        assertThat(actualPage.getTotalElements()).isEqualTo(3);
        verify(itemRepository).findAll(any(Pageable.class));
        verify(itemRepository, never()).searchItems(anyString(), any(Pageable.class));
    }


    @Test
    @DisplayName("Должен выполнить поиск товаров")
    void shouldSearchItems() {
        String searchQuery = "мяч";
        int pageNumber = 1;
        int pageSize = 5;

        Page<Item> emptyPage = Page.empty();
        when(itemRepository.searchItems(eq(searchQuery), any(Pageable.class)))
                .thenReturn(emptyPage);

        itemService.getItems(searchQuery, "NO", pageNumber, pageSize);

        verify(itemRepository).searchItems(eq(searchQuery), any(Pageable.class));
        verify(itemRepository, never()).findAll(any(Pageable.class));
    }


    @Test
    @DisplayName("Должен получить товар по ID")
    void shouldGetItemById() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem1));

        Item foundItem = itemService.getItemById(1L);

        assertThat(foundItem).isNotNull();
        assertThat(foundItem.getTitle()).isEqualTo("Футбольный мяч");
    }

    @Test
    @DisplayName("Должен выбросить исключение при отсутствии товара")
    void shouldThrowExceptionWhenItemNotFound() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItemById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Товар не найден");
    }

    @Test
    @DisplayName("Должен получить все товары")
    void shouldGetAllItems() {
        List<Item> expectedItems = Arrays.asList(testItem1, testItem2, testItem3);
        when(itemRepository.findAll()).thenReturn(expectedItems);

        List<Item> actualItems = itemService.getAllItems();

        assertThat(actualItems).hasSize(3);
        verify(itemRepository).findAll(); // Без параметров
    }

    @Test
    @DisplayName("Должен сохранить товар")
    void shouldSaveItem() {
        Item newItem = new Item();
        newItem.setTitle("Новый товар");
        newItem.setPrice(500L);
        newItem.setStock(10);

        when(itemRepository.save(any(Item.class))).thenReturn(newItem);

        Item savedItem = itemService.saveItem(newItem);

        assertThat(savedItem.getTitle()).isEqualTo("Новый товар");
        verify(itemRepository).save(newItem);
    }

}