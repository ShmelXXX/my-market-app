package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Flux<Item> getItems(String search, String sort, int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;

        if (search != null && !search.trim().isEmpty()) {
            // Поиск с сортировкой
            if ("ALPHA".equalsIgnoreCase(sort)) {
                return itemRepository.searchItemsOrderByTitle(search.trim(), pageSize, offset);
            } else if ("PRICE".equalsIgnoreCase(sort)) {
                return itemRepository.searchItemsOrderByPrice(search.trim(), pageSize, offset);
            } else {
                return itemRepository.searchItems(search.trim(), pageSize, offset);
            }
        } else {
            // Все товары с сортировкой
            if ("ALPHA".equalsIgnoreCase(sort)) {
                return itemRepository.findAllOrderByTitle(pageSize, offset);
            } else if ("PRICE".equalsIgnoreCase(sort)) {
                return itemRepository.findAllOrderByPrice(pageSize, offset);
            } else {
                return itemRepository.findAllWithPagination(pageSize, offset);
            }
        }
    }

    public Mono<Long> getTotalItems(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return itemRepository.countSearchItems(search.trim());
        } else {
            return itemRepository.countAllItems();
        }
    }

    public Mono<Item> getItemById(Long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Товар с ID " + id + " не найден")));
    }

    public Flux<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Mono<Item> saveItem(Item item) {
        if (item.getId() == null) {
            // Новый товар
            return itemRepository.save(item);
        } else {
            // Обновление существующего товара
            return itemRepository.findById(item.getId())
                    .switchIfEmpty(Mono.error(new RuntimeException("Товар с ID " + item.getId() + " не найден")))
                    .flatMap(existingItem -> {
                        existingItem.setTitle(item.getTitle());
                        existingItem.setDescription(item.getDescription());
                        existingItem.setImgPath(item.getImgPath());
                        existingItem.setPrice(item.getPrice());
                        existingItem.setStock(item.getStock());
                        return itemRepository.save(existingItem);
                    });
        }
    }

    public Flux<Item> getItemsInStock() {
        return itemRepository.findByStockGreaterThan(0);
    }

    public Mono<Boolean> existsById(Long id) {
        return itemRepository.existsById(id);
    }
}