package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;

@Controller
@RequestMapping("/buy")
@RequiredArgsConstructor
public class BuyController {

    private final OrderService orderService;
    private final CartService cartService;

    @PostMapping
    public String handleBuy(HttpServletRequest request) {
        String sessionId = cartService.getSessionId(request);
        Order order = orderService.createOrder(sessionId);
        return "redirect:/orders/" + order.getId() + "?newOrder=true";
    }
}