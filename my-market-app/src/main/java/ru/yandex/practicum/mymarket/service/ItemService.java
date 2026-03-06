package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemCacheService itemCacheService;


   @Cacheable(value = "itemsPage", key = "{#search, #sort, #pageNumber, #pageSize}")
    public Page<Item> getItems(String search, String sort, int pageNumber, int pageSize) {
        Pageable pageable = createPageable(sort, pageNumber, pageSize);

        if (search != null && !search.trim().isEmpty()) {
            return itemRepository.searchItems(search.trim(), pageable);
        } else {
            return itemRepository.findAll(pageable);
        }
    }

    public Item getItemById(Long id) {
        // Используем кэширование
        return itemCacheService.getItemById(id);
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    private Pageable createPageable(String sort, int pageNumber, int pageSize) {
        Sort sorting = Sort.unsorted();

        if (sort != null) {
            sorting = switch (sort.toUpperCase()) {
                case "ALPHA" -> Sort.by("title").ascending();
                case "PRICE" -> Sort.by("price").ascending();
                default -> Sort.unsorted();
            };
        }

        return PageRequest.of(pageNumber - 1, pageSize, sorting);
    }

    public Item saveItem(Item item) {
        Item savedItem = itemRepository.save(item);
        // Обновляем кэш
        itemCacheService.updateItemInCache(savedItem);
        // Очищаем кэш страниц, так как данные изменились
        itemCacheService.evictAllItems();
        return savedItem;
    }
}