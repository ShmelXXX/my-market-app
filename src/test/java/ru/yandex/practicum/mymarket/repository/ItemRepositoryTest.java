package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.Item;
import static org.assertj.core.api.Assertions.assertThat;


@DataR2dbcTest
@DisplayName("Реактивные тесты для ItemRepository")
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        // Очищаем базу данных перед каждым тестом
        itemRepository.deleteAll().block();

        item1 = new Item();
        item1.setTitle("Товар 1");
        item1.setDescription("Товар 1. Описание");
        item1.setImgPath("images/image1.png");
        item1.setPrice(2500L);
        item1.setStock(10);

        item2 = new Item();
        item2.setTitle("Товар 2");
        item2.setDescription("Товар 2. Описание");
        item2.setImgPath("images/image2.png");
        item2.setPrice(3000L);
        item2.setStock(5);

        // Сохраняем тестовые данные
        itemRepository.save(item1).block();
        itemRepository.save(item2).block();
    }

    @Test
    @DisplayName("Должен найти все товары с пагинацией")
    void testFindAllWithPagination() {
        // Act
        Flux<Item> items = itemRepository.findAllWithPagination(10, 0);

        // Assert
        StepVerifier.create(items)
                .expectNextMatches(item -> item.getTitle().equals("Товар 1"))
                .expectNextMatches(item -> item.getTitle().equals("Товар 2"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен подсчитать общее количество товаров")
    void testCountAllItems() {
        // Act
        Mono<Long> count = itemRepository.countAllItems();

        // Assert
        StepVerifier.create(count)
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен найти товары по поисковому запросу")
    void testSearchItems() {
        // Act
        Flux<Item> result = itemRepository.searchItems("товар", 10, 0);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(item -> item.getTitle().equals("Товар 1"))
                .expectNextMatches(item -> item.getTitle().equals("Товар 2"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен найти товары по поисковому запросу с сортировкой по названию")
    void testSearchItemsOrderByTitle() {
        // Act
        Flux<Item> result = itemRepository.searchItemsOrderByTitle("товар", 10, 0);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(item -> item.getTitle().equals("Товар 1"))
                .expectNextMatches(item -> item.getTitle().equals("Товар 2"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен найти товары по поисковому запросу с сортировкой по цене")
    void testSearchItemsOrderByPrice() {
        // Act
        Flux<Item> result = itemRepository.searchItemsOrderByPrice("товар", 10, 0);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(item -> item.getPrice() == 2500L)
                .expectNextMatches(item -> item.getPrice() == 3000L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен вернуть пустой результат для несуществующего поискового запроса")
    void testSearchItemsNoResults() {
        // Act
        Flux<Item> result = itemRepository.searchItems("несуществующий", 10, 0);

        // Assert
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен найти товар по ID")
    void testFindById() {
        // Arrange
        Long savedId = item1.getId();

        // Act
        Mono<Item> found = itemRepository.findById(savedId);

        // Assert
        StepVerifier.create(found)
                .assertNext(item -> {
                    assertThat(item.getId()).isEqualTo(savedId);
                    assertThat(item.getTitle()).isEqualTo("Товар 1");
                    assertThat(item.getDescription()).isEqualTo("Товар 1. Описание");
                    assertThat(item.getPrice()).isEqualTo(2500L);
                    assertThat(item.getStock()).isEqualTo(10);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен сохранить новый товар")
    void testSaveItem() {
        // Arrange
        Item newItem = new Item();
        newItem.setTitle("Товар 3");
        newItem.setDescription("Товар 3. Описание");
        newItem.setImgPath("images/image3.png");
        newItem.setPrice(7500L);
        newItem.setStock(3);

        // Act
        Mono<Item> saved = itemRepository.save(newItem);

        // Assert
        StepVerifier.create(saved)
                .assertNext(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getTitle()).isEqualTo("Товар 3");
                    assertThat(item.getDescription()).isEqualTo("Товар 3. Описание");
                    assertThat(item.getPrice()).isEqualTo(7500L);
                    assertThat(item.getStock()).isEqualTo(3);
                })
                .verifyComplete();

        // Проверяем, что товар действительно сохранился
        StepVerifier.create(itemRepository.countAllItems())
                .expectNext(3L)
                .verifyComplete();
    }
}