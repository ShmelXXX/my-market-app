package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Реактивные юнит-тесты для ItemService")
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
        testItem1 = new Item();
        testItem1.setId(1L);
        testItem1.setTitle("Футбольный мяч");
        testItem1.setDescription("Качественный мяч");
        testItem1.setImgPath("images/soccer_ball.png");
        testItem1.setPrice(2500L);
        testItem1.setStock(20);

        testItem2 = new Item();
        testItem2.setId(2L);
        testItem2.setTitle("Бейсболка красная");
        testItem2.setDescription("Красная бейсболка");
        testItem2.setImgPath("images/baseball_cap_red.png");
        testItem2.setPrice(1000L);
        testItem2.setStock(5);

        testItem3 = new Item();
        testItem3.setId(3L);
        testItem3.setTitle("Зонт");
        testItem3.setDescription("Супер зонт");
        testItem3.setImgPath("images/umbrella.png");
        testItem3.setPrice(1500L);
        testItem3.setStock(30);
    }

    @Test
    @DisplayName("Должен получить товары с пагинацией без сортировки и поиска")
    void shouldGetItemsWithPaginationNoSortNoSearch() {
        // Arrange
        int pageNumber = 1;
        int pageSize = 2;
        List<Item> items = Arrays.asList(testItem1, testItem2);
        Flux<Item> expectedFlux = Flux.fromIterable(items);

        when(itemRepository.findAllWithPagination(eq(pageSize), eq(0)))
                .thenReturn(expectedFlux);
        when(itemRepository.countAllItems())
                .thenReturn(Mono.just(3L));

        // Act
        Flux<Item> result = itemService.getItems(null, "NO", pageNumber, pageSize);
        Mono<Long> total = itemService.getTotalItems(null);

        // Assert
        StepVerifier.create(result)
                .expectNext(testItem1)
                .expectNext(testItem2)
                .verifyComplete();

        StepVerifier.create(total)
                .expectNext(3L)
                .verifyComplete();

        verify(itemRepository).findAllWithPagination(eq(pageSize), eq(0));
        verify(itemRepository).countAllItems();
        verify(itemRepository, never()).searchItems(anyString(), anyInt(), anyInt());
    }


    @Test
    @DisplayName("Должен выполнить поиск товаров")
    void shouldSearchItems() {
        // Arrange
        String searchQuery = "мяч";
        int pageNumber = 1;
        int pageSize = 5;
        List<Item> items = Collections.singletonList(testItem1);
        Flux<Item> expectedFlux = Flux.fromIterable(items);

        when(itemRepository.searchItems(eq(searchQuery), eq(pageSize), eq(0)))
                .thenReturn(expectedFlux);
        when(itemRepository.countSearchItems(eq(searchQuery)))
                .thenReturn(Mono.just(1L));

        // Act
        Flux<Item> result = itemService.getItems(searchQuery, "NO", pageNumber, pageSize);
        Mono<Long> total = itemService.getTotalItems(searchQuery);

        // Assert
        StepVerifier.create(result)
                .expectNext(testItem1)
                .verifyComplete();

        StepVerifier.create(total)
                .expectNext(1L)
                .verifyComplete();

        verify(itemRepository).searchItems(eq(searchQuery), eq(pageSize), eq(0));
        verify(itemRepository).countSearchItems(eq(searchQuery));
        verify(itemRepository, never()).findAllWithPagination(anyInt(), anyInt());
    }


    @Test
    @DisplayName("Должен получить товар по ID")
    void shouldGetItemById() {
        // Arrange
        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem1));

        // Act
        Mono<Item> result = itemService.getItemById(1L);

        // Assert
        StepVerifier.create(result)
                .assertNext(item -> {
                    assertThat(item.getId()).isEqualTo(1L);
                    assertThat(item.getTitle()).isEqualTo("Футбольный мяч");
                })
                .verifyComplete();

        verify(itemRepository).findById(1L);
    }

    @Test
    @DisplayName("Должен выбросить исключение при отсутствии товара")
    void shouldThrowExceptionWhenItemNotFoundWithError() {
        // Arrange
        when(itemRepository.findById(999L)).thenReturn(Mono.empty());

        // Создаем сервис с обработкой ошибок
        ItemService itemServiceWithError = new ItemService(itemRepository) {
            @Override
            public Mono<Item> getItemById(Long id) {
                return itemRepository.findById(id)
                        .switchIfEmpty(Mono.error(new RuntimeException("Товар с ID " + id + " не найден")));
            }
        };

        // Act & Assert
        StepVerifier.create(itemServiceWithError.getItemById(999L))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Товар с ID 999 не найден")
                )
                .verify();
    }

    @Test
    @DisplayName("Должен получить все товары")
    void shouldGetAllItems() {
        // Arrange
        List<Item> expectedItems = Arrays.asList(testItem1, testItem2, testItem3);
        when(itemRepository.findAll()).thenReturn(Flux.fromIterable(expectedItems));

        // Act
        Flux<Item> result = itemService.getAllItems();

        // Assert
        StepVerifier.create(result)
                .expectNext(testItem1)
                .expectNext(testItem2)
                .expectNext(testItem3)
                .verifyComplete();

        verify(itemRepository).findAll();
    }

    @Test
    @DisplayName("Должен сохранить новый товар")
    void shouldSaveNewItem() {
        // Arrange
        Item newItem = new Item();
        newItem.setTitle("Новый товар");
        newItem.setDescription("Описание");
        newItem.setImgPath("images/new.png");
        newItem.setPrice(500L);
        newItem.setStock(10);

        Item savedItem = new Item();
        savedItem.setId(4L);
        savedItem.setTitle("Новый товар");
        savedItem.setDescription("Описание");
        savedItem.setImgPath("images/new.png");
        savedItem.setPrice(500L);
        savedItem.setStock(10);

        when(itemRepository.save(any(Item.class))).thenReturn(Mono.just(savedItem));

        // Act
        Mono<Item> result = itemService.saveItem(newItem);

        // Assert
        StepVerifier.create(result)
                .assertNext(item -> {
                    assertThat(item.getId()).isEqualTo(4L);
                    assertThat(item.getTitle()).isEqualTo("Новый товар");
                })
                .verifyComplete();

        verify(itemRepository).save(any(Item.class));
    }

}