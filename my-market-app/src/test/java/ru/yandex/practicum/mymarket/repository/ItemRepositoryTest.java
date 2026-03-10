package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.practicum.mymarket.model.Item;

import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest
class ItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ItemRepository itemRepository;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        item1 = new Item();
        item1.setTitle("Товар 1");
        item1.setDescription("Товар 1. Описание");
        item1.setImgPath("images/image1.png");
        item1.setPrice(2500L);
        item1.setStock(10);
        entityManager.persist(item1);

        item2 = new Item();
        item2.setTitle("Товар 2");
        item2.setDescription("Товар 2. Описание");
        item2.setImgPath("images/image2.png");
        item2.setPrice(3000L);
        item2.setStock(5);
        entityManager.persist(item2);

        entityManager.flush();
    }

    @Test
    void testFindAll() {
        Page<Item> items = itemRepository.findAll(PageRequest.of(0, 10));
        assertThat(items.getContent()).hasSize(2);
    }

    @Test
    void testSearchItems() {
        Page<Item> result = itemRepository.searchItems("товар", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void testSearchItemsNoResults() {
        Page<Item> result = itemRepository.searchItems("несуществующий", PageRequest.of(0, 10));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void testFindById() {
        Item found = itemRepository.findById(item1.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Товар 1");
    }

    @Test
    void testSaveItem() {
        Item newItem = new Item();
        newItem.setTitle("Товар 3");
        newItem.setDescription("Товар 3. Описание");
        newItem.setImgPath("images/image3.png");
        newItem.setPrice(7500L);
        newItem.setStock(3);

        Item saved = itemRepository.save(newItem);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Товар 3");
    }
}