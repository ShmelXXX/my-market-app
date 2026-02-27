package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public Mono<String> getCart(HttpServletRequest request, Model model) {
        String sessionId = cartService.getSessionId(request);

        return cartService.getCartItemsWithDetails(sessionId)
                .map(this::convertToItemDto)
                .collectList()
                .zipWith(cartService.getTotalSum(sessionId))
                .map(tuple -> {
                    List<ItemDto> itemDtos = tuple.getT1();
                    Long total = tuple.getT2();

                    model.addAttribute("items", itemDtos);
                    model.addAttribute("total", total);

                    return "cart";
                });
    }

    @PostMapping("/items")
    public Mono<String> updateCartItem(
            @RequestParam Long id,
            @RequestParam String action,
            HttpServletRequest request,
            Model model) {

        String sessionId = cartService.getSessionId(request);

        return cartService.updateCartItem(id, action, sessionId)
                .then(Mono.defer(() ->
                        cartService.getCartItemsWithDetails(sessionId)
                                .map(this::convertToItemDto)
                                .collectList()
                                .zipWith(cartService.getTotalSum(sessionId))
                                .map(tuple -> {
                                    List<ItemDto> itemDtos = tuple.getT1();
                                    Long total = tuple.getT2();

                                    model.addAttribute("items", itemDtos);
                                    model.addAttribute("total", total);

                                    return "cart";
                                })
                ));
    }

    private ItemDto convertToItemDto(CartService.CartItemWithItem cartItemWithItem) {
        return new ItemDto(
                cartItemWithItem.getItem().getId(),
                cartItemWithItem.getItem().getTitle(),
                cartItemWithItem.getItem().getDescription(),
                cartItemWithItem.getItem().getImgPath(),
                cartItemWithItem.getItem().getPrice(),
                cartItemWithItem.getQuantity()
        );
    }
}