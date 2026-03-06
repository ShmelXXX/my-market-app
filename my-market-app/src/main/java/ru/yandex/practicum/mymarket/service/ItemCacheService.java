package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemCacheService {

    private final ItemRepository itemRepository;
    private static final String ITEMS_CACHE = "items";

    @Cacheable(value = ITEMS_CACHE, key = "#id")
    public Item getItemById(Long id) {
        log.info("Получение товара из БД (не из кэша): {}", id);
        return itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));
    }

    @CachePut(value = ITEMS_CACHE, key = "#item.id")
    public Item updateItemInCache(Item item) {
        log.info("Обновление товара в кэше: {}", item.getId());
        return item;
    }

    @CacheEvict(value = ITEMS_CACHE, key = "#id")
    public void evictItemFromCache(Long id) {
        log.info("Удаление товара из кэша: {}", id);
    }

    @CacheEvict(value = ITEMS_CACHE, allEntries = true)
    public void evictAllItems() {
        log.info("Очистка всего кэша товаров");
    }
}