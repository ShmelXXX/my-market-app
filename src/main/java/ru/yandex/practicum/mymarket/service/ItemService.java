package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
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

    public Page<Item> getItems(String search, String sort, int pageNumber, int pageSize) {
        Pageable pageable = createPageable(sort, pageNumber, pageSize);

        if (search != null && !search.trim().isEmpty()) {
            return itemRepository.searchItems(search.trim(), pageable);
        } else {
            return itemRepository.findAll(pageable);
        }
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));
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
        return itemRepository.save(item);
    }
}