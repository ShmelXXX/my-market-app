package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;

@Slf4j
@Controller
@RequestMapping("/buy")
@RequiredArgsConstructor
public class BuyController {

    private final OrderService orderService;
    private final CartService cartService;

    @PostMapping
    public Mono<String> handleBuy(HttpServletRequest request) {
        String sessionId = cartService.getSessionId(request);

        return orderService.createOrder(sessionId)
                .map(order -> "redirect:/orders/" + order.getId() + "?newOrder=true")
                .onErrorResume(e -> {
                    log.error("Ошибка при создании заказа: {}", e.getMessage());
                    if (e.getMessage().contains("Корзина пуста")) {
                        return Mono.just("redirect:/cart/items?error=empty-cart");
                    }
                    return Mono.just("redirect:/cart/items?error=order-failed");
                });
    }
}