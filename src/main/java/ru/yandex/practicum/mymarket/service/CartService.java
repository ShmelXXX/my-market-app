package ru.yandex.practicum.mymarket.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public String getSessionId(HttpServletRequest request) {
        return request.getSession().getId();
    }

    public List<CartItem> getCartItems(String sessionId) {
        return cartItemRepository.findBySessionId(sessionId);
    }

    public Long getTotalSum(String sessionId) {
        List<CartItem> cartItems = getCartItems(sessionId);
        return cartItems.stream()
                .mapToLong(ci -> ci.getItem().getPrice() * ci.getQuantity())
                .sum();
    }

    @Transactional
    public void updateCartItem(Long itemId, String action, String sessionId) {
        Optional<CartItem> existingCartItem = cartItemRepository.findByItemIdAndSessionId(itemId, sessionId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        switch (action.toUpperCase()) {
            case "PLUS":
                if (existingCartItem.isPresent()) {
                    CartItem cartItem = existingCartItem.get();
                    cartItem.setQuantity(cartItem.getQuantity() + 1);
                    cartItemRepository.save(cartItem);
                } else {
                    CartItem newCartItem = new CartItem();
                    newCartItem.setItem(item);
                    newCartItem.setQuantity(1);
                    newCartItem.setSessionId(sessionId);
                    cartItemRepository.save(newCartItem);
                }
                break;

            case "MINUS":
                if (existingCartItem.isPresent()) {
                    CartItem cartItem = existingCartItem.get();
                    if (cartItem.getQuantity() > 1) {
                        cartItem.setQuantity(cartItem.getQuantity() - 1);
                        cartItemRepository.save(cartItem);
                    } else {
                        cartItemRepository.delete(cartItem);
                    }
                }
                break;

            case "DELETE":
                existingCartItem.ifPresent(cartItemRepository::delete);
                break;

            default:
                throw new IllegalArgumentException("Неверное действие: " + action);
        }
    }

    public int getItemCountInCart(Long itemId, String sessionId) {
        return cartItemRepository.findByItemIdAndSessionId(itemId, sessionId)
                .map(CartItem::getQuantity)
                .orElse(0);
    }

    @Transactional
    public void clearCart(String sessionId) {
        cartItemRepository.deleteBySessionId(sessionId);
    }
}