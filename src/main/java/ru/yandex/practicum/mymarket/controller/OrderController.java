package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
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
    public Mono<String> getOrders(Model model) {
        return orderService.getAllOrders()
                .flatMap(order ->
                        orderService.getOrderItems(order.getId())
                                .collectList()
                                .map(items -> convertToOrderDto(order, items))
                )
                .collectList()
                .map(orderDtos -> {
                    model.addAttribute("orders", orderDtos);
                    return "orders";
                });
    }

    @GetMapping("/{id}")
    public Mono<String> getOrder(
            @PathVariable
            @Min(value = 1, message = "ID заказа должен быть положительным")
            Long id,
            @RequestParam(required = false, defaultValue = "false") boolean newOrder,
            Model model) {

        return orderService.getOrderById(id)
                .flatMap(order ->
                        orderService.getOrderItems(order.getId())
                                .collectList()
                                .map(items -> {
                                    OrderDto orderDto = convertToOrderDto(order, items);
                                    model.addAttribute("order", orderDto);
                                    model.addAttribute("newOrder", newOrder);
                                    return "order";
                                })
                )
                .switchIfEmpty(Mono.error(new RuntimeException("Заказ с ID " + id + " не найден")));
    }

    @PostMapping("/buy")
    public Mono<String> buy(HttpServletRequest request) {
        String sessionId = cartService.getSessionId(request);
        return orderService.createOrder(sessionId)
                .map(order -> "redirect:/orders/" + order.getId() + "?newOrder=true");
    }

    private OrderDto convertToOrderDto(Order order, List<OrderItem> items) {
        List<ItemDto> itemDtos = items.stream()
                .map(orderItem -> new ItemDto(
                        orderItem.getItemId(),
                        getItemTitle(orderItem),
                        null,
                        null,
                        orderItem.getPrice(),
                        orderItem.getQuantity()
                ))
                .collect(Collectors.toList());

        return new OrderDto(order.getId(), itemDtos, order.getTotalSum());
    }

    private String getItemTitle(OrderItem orderItem) {
        if (orderItem.getItem() != null) {
            return orderItem.getItem().getTitle();
        }
        return "Товар #" + orderItem.getItemId();
    }
}