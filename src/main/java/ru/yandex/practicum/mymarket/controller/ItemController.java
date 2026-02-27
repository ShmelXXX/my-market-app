package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.PagingDto;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping
@RequiredArgsConstructor
@Validated
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping({"/", "/items"})
    public Mono<String> getItems(
            @RequestParam(required = false)
            @Size(max = 100, message = "Поисковый запрос не может превышать 100 символов")
            String search,

            @RequestParam(required = false, defaultValue = "NO")
            String sort,

            @RequestParam(required = false, defaultValue = "1")
            @Min(value = 1, message = "Номер страницы должен быть не менее 1")
            int pageNumber,

            @RequestParam(required = false, defaultValue = "5")
            @Min(value = 1, message = "Размер страницы должен быть не менее 1")
            @Max(value = 100, message = "Размер страницы не может превышать 100")
            int pageSize,

            HttpServletRequest request,
            Model model) {

        String sessionId = cartService.getSessionId(request);

        // Параллельно загружаем товары и общее количество
        Flux<Item> itemsFlux = itemService.getItems(search, sort, pageNumber, pageSize);
        Mono<Long> totalItemsMono = itemService.getTotalItems(search);

        return itemsFlux.collectList()
                .zipWith(totalItemsMono)
                .flatMap(tuple -> {
                    List<Item> items = tuple.getT1();
                    Long totalItems = tuple.getT2();

                    // Для каждого товара получаем количество в корзине
                    Flux<ItemDto> itemDtosFlux = Flux.fromIterable(items)
                            .flatMap(item ->
                                    cartService.getItemCountInCart(item.getId(), sessionId)
                                            .map(count -> new ItemDto(
                                                    item.getId(),
                                                    item.getTitle(),
                                                    item.getDescription(),
                                                    item.getImgPath(),
                                                    item.getPrice(),
                                                    count
                                            ))
                            );

                    return itemDtosFlux.collectList()
                            .map(itemDtos -> {
                                // Группируем товары по 3 в строку
                                List<List<ItemDto>> groupedItems = groupItemsByRow(itemDtos, 3);

                                int totalPages = (int) Math.ceil((double) totalItems / pageSize);
                                PagingDto paging = new PagingDto(
                                        pageSize,
                                        pageNumber,
                                        pageNumber > 1,
                                        pageNumber < totalPages
                                );

                                model.addAttribute("items", groupedItems);
                                model.addAttribute("search", search);
                                model.addAttribute("sort", sort);
                                model.addAttribute("paging", paging);

                                return "items";
                            });
                })
                .onErrorResume(e -> {
                    log.error("Ошибка при загрузке товаров: {}", e.getMessage());
                    model.addAttribute("error", "Не удалось загрузить товары");
                    model.addAttribute("items", new ArrayList<>());
                    model.addAttribute("paging", new PagingDto(pageSize, pageNumber, false, false));
                    return Mono.just("items");
                });
    }

    @PostMapping("/items")
    public Mono<String> updateCartItemFromItems(
            @RequestParam
            @Min(value = 1, message = "ID товара должен быть положительным")
            Long id,

            @RequestParam String action,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            HttpServletRequest request) {

        String sessionId = cartService.getSessionId(request);

        return cartService.updateCartItem(id, action, sessionId)
                .then(Mono.just(buildRedirectUrl(id, search, sort, pageNumber, pageSize)));
    }

    @GetMapping("/items/{id}")
    public Mono<String> getItem(
            @PathVariable
            @Min(value = 1, message = "ID товара должен быть положительным")
            Long id,

            HttpServletRequest request,
            Model model) {

        String sessionId = cartService.getSessionId(request);

        return itemService.getItemById(id)
                .flatMap(item ->
                        cartService.getItemCountInCart(id, sessionId)
                                .map(count -> {
                                    ItemDto itemDto = new ItemDto(
                                            item.getId(),
                                            item.getTitle(),
                                            item.getDescription(),
                                            item.getImgPath(),
                                            item.getPrice(),
                                            count
                                    );

                                    model.addAttribute("item", itemDto);
                                    return "item";
                                })
                )
                .switchIfEmpty(Mono.error(new RuntimeException("Товар с ID " + id + " не найден")))
                .onErrorResume(e -> {
                    log.error("Ошибка при загрузке товара: {}", e.getMessage());
                    model.addAttribute("error", "Товар не найден");
                    return Mono.just("error");
                });
    }

    @PostMapping("/items/{id}")
    public Mono<String> updateCartItemFromItem(
            @PathVariable
            @Min(value = 1, message = "ID товара должен быть положительным")
            Long id,

            @RequestParam String action,
            HttpServletRequest request,
            Model model) {

        String sessionId = cartService.getSessionId(request);

        return cartService.updateCartItem(id, action, sessionId)
                .then(Mono.defer(() ->
                        itemService.getItemById(id)
                                .flatMap(item ->
                                        cartService.getItemCountInCart(id, sessionId)
                                                .map(count -> {
                                                    ItemDto itemDto = new ItemDto(
                                                            item.getId(),
                                                            item.getTitle(),
                                                            item.getDescription(),
                                                            item.getImgPath(),
                                                            item.getPrice(),
                                                            count
                                                    );

                                                    model.addAttribute("item", itemDto);
                                                    return "item";
                                                })
                                )
                ));
    }

    @PostMapping("/admin/items/add")
    public Mono<String> addItem(
            @RequestParam
            @Size(min = 3, max = 100, message = "Название товара должно содержать от 3 до 100 символов")
            String title,

            @RequestParam
            @Size(max = 1000, message = "Описание товара не может превышать 1000 символов")
            String description,

            @RequestParam
            @Size(max = 255, message = "Путь к изображению не может превышать 255 символов")
            String imgPath,

            @RequestParam
            @Positive(message = "Цена товара должна быть положительной")
            @Max(value = 1000000000L, message = "Цена товара не может превышать 1 000 000 000")
            Long price,

            @RequestParam
            @Min(value = 0, message = "Количество товара не может быть отрицательным")
            @Max(value = 100000, message = "Количество товара не может превышать 100 000")
            Integer stock,

            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            HttpServletRequest request) {

        Item item = new Item();
        item.setTitle(title);
        item.setDescription(description);
        item.setImgPath(imgPath);
        item.setPrice(price);
        item.setStock(stock);

        return itemService.saveItem(item)
                .then(Mono.just("redirect:/items?sort=" + sort +
                        "&pageNumber=" + pageNumber +
                        "&pageSize=" + pageSize +
                        (search != null ? "&search=" + search : "")));
    }

    // Вспомогательный метод для группировки товаров по рядам
    private List<List<ItemDto>> groupItemsByRow(List<ItemDto> items, int itemsPerRow) {
        List<List<ItemDto>> groupedItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i += itemsPerRow) {
            List<ItemDto> row = new ArrayList<>(items.subList(i, Math.min(i + itemsPerRow, items.size())));
            groupedItems.add(row);
        }

        // Добавляем заглушки, если последняя строка неполная
        if (!groupedItems.isEmpty()) {
            List<ItemDto> lastRow = groupedItems.getLast();
            while (lastRow.size() < itemsPerRow) {
                lastRow.add(new ItemDto(-1L, "", "", "", 0L, 0));
            }
        }

        return groupedItems;
    }

    // Вспомогательный метод для построения URL редиректа
    private String buildRedirectUrl(Long id, String search, String sort, int pageNumber, int pageSize) {
        StringBuilder redirect = new StringBuilder("redirect:/items");
        redirect.append("?id=").append(id);
        if (search != null && !search.trim().isEmpty()) {
            redirect.append("&search=").append(search);
        }
        redirect.append("&sort=").append(sort);
        redirect.append("&pageNumber=").append(pageNumber);
        redirect.append("&pageSize=").append(pageSize);
        return redirect.toString();
    }
}