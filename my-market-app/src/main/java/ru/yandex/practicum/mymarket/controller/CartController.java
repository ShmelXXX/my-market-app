package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public String getCart(HttpServletRequest request, Model model) {
        String sessionId = cartService.getSessionId(request);
        List<CartItem> cartItems = cartService.getCartItems(sessionId);

        List<ItemDto> itemDtos = cartItems.stream()
                .map(ci -> new ItemDto(
                        ci.getItem().getId(),
                        ci.getItem().getTitle(),
                        ci.getItem().getDescription(),
                        ci.getItem().getImgPath(),
                        ci.getItem().getPrice(),
                        ci.getQuantity()
                ))
                .collect(Collectors.toList());

        Long total = cartService.getTotalSum(sessionId);

        model.addAttribute("items", itemDtos);
        model.addAttribute("total", total != null ? total : 0L);

        return "cart";
    }

    @PostMapping("/items")
    public String updateCartItem(
            @RequestParam Long id,
            @RequestParam String action,
            HttpServletRequest request,
            Model model) {

        String sessionId = cartService.getSessionId(request);
        cartService.updateCartItem(id, action, sessionId);

        List<CartItem> cartItems = cartService.getCartItems(sessionId);
        List<ItemDto> itemDtos = cartItems.stream()
                .map(ci -> new ItemDto(
                        ci.getItem().getId(),
                        ci.getItem().getTitle(),
                        ci.getItem().getDescription(),
                        ci.getItem().getImgPath(),
                        ci.getItem().getPrice(),
                        ci.getQuantity()
                ))
                .collect(Collectors.toList());

        Long total = cartService.getTotalSum(sessionId);

        model.addAttribute("items", itemDtos);
        model.addAttribute("total", total);

        return "cart";
    }
}