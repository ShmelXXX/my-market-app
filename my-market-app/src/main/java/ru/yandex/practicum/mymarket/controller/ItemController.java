package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.PagingDto;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping
@RequiredArgsConstructor

@Validated

public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping({"/", "/items"})
    public String getItems(
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
        Page<Item> itemPage = itemService.getItems(search, sort, pageNumber, pageSize);

        // Группируем товары по 3 в строку
        List<ItemDto> itemDtos = itemPage.getContent().stream()
                .map(item -> {
                    int count = cartService.getItemCountInCart(item.getId(), sessionId);
                    return new ItemDto(
                            item.getId(),
                            item.getTitle(),
                            item.getDescription(),
                            item.getImgPath(),
                            item.getPrice(),
                            count
                    );
                })
                .collect(Collectors.toList());

        List<List<ItemDto>> items = new ArrayList<>();
        for (int i = 0; i < itemDtos.size(); i += 3) {
            List<ItemDto> row = new ArrayList<>(itemDtos.subList(i, Math.min(i + 3, itemDtos.size())));
            items.add(row);
        }

        // Добавляем заглушки, если последняя строка неполная
        if (!items.isEmpty()) {
            List<ItemDto> lastRow = items.getLast();
            while (lastRow.size() < 3) {
                lastRow.add(new ItemDto(-1L, "", "", "", 0L, 0));
            }
        }

        PagingDto paging = new PagingDto(
                pageSize,
                pageNumber,
                pageNumber > 1,
                pageNumber < itemPage.getTotalPages()
        );

        model.addAttribute("items", items);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", paging);

        return "items";
    }

    @PostMapping("/items")
    public String updateCartItemFromItems(
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
        cartService.updateCartItem(id, action, sessionId);

        StringBuilder redirect = new StringBuilder("redirect:/items");
        redirect.append("?id=").append(id);
        if (search != null) redirect.append("&search=").append(search);
        redirect.append("&sort=").append(sort);
        redirect.append("&pageNumber=").append(pageNumber);
        redirect.append("&pageSize=").append(pageSize);

        return redirect.toString();
    }

    @GetMapping("/items/{id}")
    public String getItem(
            @PathVariable
            @Min(value = 1, message = "ID товара должен быть положительным")
            Long id,

            HttpServletRequest request,
            Model model) {

        String sessionId = cartService.getSessionId(request);
        Item item = itemService.getItemById(id);

        ItemDto itemDto = new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                cartService.getItemCountInCart(id, sessionId)
        );

        model.addAttribute("item", itemDto);
        return "item";
    }

    @PostMapping("/items/{id}")
    public String updateCartItemFromItem(
            @PathVariable
            @Min(value = 1, message = "ID товара должен быть положительным")
            Long id,

            @RequestParam String action,
            HttpServletRequest request,
            Model model) {

        String sessionId = cartService.getSessionId(request);
        cartService.updateCartItem(id, action, sessionId);

        Item item = itemService.getItemById(id);
        ItemDto itemDto = new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                cartService.getItemCountInCart(id, sessionId)
        );

        model.addAttribute("item", itemDto);
        return "item";
    }

    @PostMapping("/admin/items/add")
    public String addItem(
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
            @Min(value = 0, message = "-Количество товара не может быть отрицательным")
            @Max(value = 100000, message = "Количество товара не может превышать 100 000")
            Integer stock,

            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,

            @RequestParam(required = false, defaultValue = "1")
            @Min(value = 1, message = "Номер страницы должен быть не менее 1")
            int pageNumber,

            @RequestParam(required = false, defaultValue = "5")
            @Min(value = 1, message = "Размер страницы должен быть не менее 1")
            @Max(value = 100, message = "Размер страницы не может превышать 100")
            int pageSize,

            HttpServletRequest request) {

        Item item = new Item();
        item.setTitle(title);
        item.setDescription(description);

        item.setImgPath(imgPath);

        item.setPrice(price);
        item.setStock(stock);

        itemService.saveItem(item);

        return "redirect:/items?sort=" + sort +
                "&pageNumber=" + pageNumber +
                "&pageSize=" + pageSize +
                (search != null ? "&search=" + search : "");
    }
}