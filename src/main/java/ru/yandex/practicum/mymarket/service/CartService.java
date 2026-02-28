package ru.yandex.practicum.mymarket.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public String getSessionId(HttpServletRequest request) {
        return request.getSession().getId();
    }

    public Flux<CartItem> getCartItems(String sessionId) {
        return cartItemRepository.findBySessionId(sessionId);
    }

    public Flux<CartItemWithItem> getCartItemsWithDetails(String sessionId) {
        return cartItemRepository.findBySessionId(sessionId)
                .flatMap(this::enrichCartItemWithItemDetails);
    }

    private Mono<CartItemWithItem> enrichCartItemWithItemDetails(CartItem cartItem) {
        return itemRepository.findById(cartItem.getItemId())
                .map(item -> new CartItemWithItem(cartItem, item))
                .switchIfEmpty(Mono.error(new RuntimeException("Товар с ID " + cartItem.getItemId() + " не найден")));
    }

    public Mono<Long> getTotalSum(String sessionId) {
        return getCartItemsWithDetails(sessionId)
                .map(cartItemWithItem ->
                        cartItemWithItem.getItem().getPrice() * cartItemWithItem.getQuantity()
                )
                .reduce(0L, Long::sum);
    }

    public Mono<Void> updateCartItem(Long itemId, String action, String sessionId) {
        if (itemId == null || itemId <= 0) {
            return Mono.error(new IllegalArgumentException("Некорректный ID товара"));
        }

        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Некорректный ID сессии"));
        }

        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Товар с ID " + itemId + " не найден")))
                .flatMap(item ->
                        cartItemRepository.findBySessionIdAndItemId(sessionId, itemId)
                                .flatMap(existingCartItem -> processExistingCartItem(existingCartItem, action, item))
                                .switchIfEmpty(processNewCartItem(action, item, sessionId))
                )
                .then();
    }

    private Mono<CartItem> processExistingCartItem(CartItem cartItem, String action, Item item) {
        switch (action.toUpperCase()) {
            case "PLUS":
                if (cartItem.getQuantity() >= item.getStock()) {
                    return Mono.error(new IllegalStateException("Недостаточно товара на складе. Доступно: " + item.getStock()));
                }
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                return cartItemRepository.save(cartItem);

            case "MINUS":
                if (cartItem.getQuantity() > 1) {
                    cartItem.setQuantity(cartItem.getQuantity() - 1);
                    return cartItemRepository.save(cartItem);
                } else {
                    return cartItemRepository.delete(cartItem).then(Mono.empty());
                }

            case "DELETE":
                return cartItemRepository.delete(cartItem).then(Mono.empty());

            default:
                return Mono.error(new IllegalArgumentException("Неверное действие: " + action));
        }
    }

    private Mono<CartItem> processNewCartItem(String action, Item item, String sessionId) {
        if (!"PLUS".equalsIgnoreCase(action)) {
            return Mono.empty(); // Для других действий просто ничего не делаем
        }

        if (item.getStock() < 1) {
            return Mono.error(new IllegalStateException("Товар отсутствует на складе"));
        }

        CartItem newCartItem = new CartItem();
        newCartItem.setItemId(item.getId());
        newCartItem.setQuantity(1);
        newCartItem.setSessionId(sessionId);

        return cartItemRepository.save(newCartItem);
    }

    public Mono<Integer> getItemCountInCart(Long itemId, String sessionId) {
        return cartItemRepository.findBySessionIdAndItemId(sessionId, itemId)
                .map(CartItem::getQuantity)
                .defaultIfEmpty(0);
    }

    public Mono<Void> clearCart(String sessionId) {
        return cartItemRepository.deleteBySessionId(sessionId);
    }

    // Вспомогательный класс для передачи CartItem вместе с Item
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CartItemWithItem {
        private CartItem cartItem;
        private Item item;

        public Long getItemId() {
            return cartItem.getItemId();
        }

        public Integer getQuantity() {
            return cartItem.getQuantity();
        }
    }
}