package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.yandex.practicum.mymarket.model.Order;
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
    public String handleBuy(HttpServletRequest request, Model model) {
        log.info("Processing buy request");

        // Проверка, авторизован ли пользователь
        if (!cartService.isAuthenticated()) {
            log.warn("User not authenticated, redirecting to login");
            return "redirect:/login";
        }

        String sessionId = cartService.getSessionId(request);
        Long total = cartService.getTotalSum(sessionId);
        log.info("Total sum: {}", total);

        // String userId = "user1"; // Заглушка
        String userId = cartService.getCurrentUser().getUsername();
        log.info("User ID: {}", userId);

        // Проверяем баланс перед оформлением заказа
        if (!orderService.checkBalance(userId, total)) {
            model.addAttribute("error", "Недостаточно средств или сервис платежей недоступен");
            return "cart"; // Возвращаемся в корзину с ошибкой
        }

        Order order = orderService.createOrder(sessionId, userId);
        return "redirect:/orders/" + order.getId() + "?newOrder=true";
    }
}