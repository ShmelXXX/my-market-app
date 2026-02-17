package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor

@Validated

public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    @GetMapping
    public String getOrders(Model model) {
        List<Order> orders = orderService.getAllOrders();

        List<OrderDto> orderDtos = orders.stream()
                .map(this::convertToOrderDto)
                .collect(Collectors.toList());

        model.addAttribute("orders", orderDtos);
        return "orders";
    }

    @GetMapping("/{id}")
    public String getOrder(
            @PathVariable
            @Min(value = 1, message = "ID заказа должен быть положительным")
            Long id,

            @RequestParam(required = false, defaultValue = "false") boolean newOrder,
            Model model) {

        Order order = orderService.getOrderById(id);

        OrderDto orderDto = convertToOrderDto(order);

        model.addAttribute("order", orderDto);
        model.addAttribute("newOrder", newOrder);

        return "order";
    }

    @PostMapping("/buy")
    public String buy(HttpServletRequest request) {
        String sessionId = cartService.getSessionId(request);
        Order order = orderService.createOrder(sessionId);

        return "redirect:/orders/" + order.getId() + "?newOrder=true";
    }

    private OrderDto convertToOrderDto(Order order) {
        List<ItemDto> itemDtos = order.getItems().stream()
                .map(oi -> new ItemDto(
                        oi.getItem().getId(),
                        oi.getItem().getTitle(),
                        null,
                        null,
                        oi.getPrice(),
                        oi.getQuantity()
                ))
                .collect(Collectors.toList());

        return new OrderDto(order.getId(), itemDtos, order.getTotalSum());
    }

}