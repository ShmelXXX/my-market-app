package ru.yandex.practicum.mymarket.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.UserRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public String getSessionId(HttpServletRequest request) {
        return request.getSession().getId();
    }

    public User getCurrentUser() {
        Object principal = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return userRepository.findByUsername(username).orElse(null);
        }
        return null;
    }

    public boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    public List<CartItem> getCartItems(String sessionId) {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            log.info("Getting cart for user: {}", currentUser.getUsername());
            return cartItemRepository.findByUser(currentUser);
        } else {
            log.info("Getting cart for anonymous session: {}", sessionId);
            return cartItemRepository.findBySessionId(sessionId);
        }
    }

    public Long getTotalSum(String sessionId) {
        List<CartItem> cartItems = getCartItems(sessionId);
        return cartItems.stream()
                .mapToLong(ci -> ci.getItem().getPrice() * ci.getQuantity())
                .sum();
    }

    @Transactional
    public void updateCartItem(Long itemId, String action, String sessionId) {

        log.info("Updating cart item: itemId={}, action={}, sessionId={}", itemId, action, sessionId);

        if (itemId == null || itemId <= 0) {
            throw new IllegalArgumentException("Некорректный ID товара");
        }

        User currentUser = getCurrentUser();
        if (currentUser == null && (sessionId == null || sessionId.trim().isEmpty())) {
            throw new IllegalArgumentException("Некорректный ID сессии");
        }

        Optional<CartItem> existingCartItem;
        if (currentUser != null) {
            existingCartItem = cartItemRepository.findByItemIdAndUser(itemId, currentUser);
        } else {
            existingCartItem = cartItemRepository.findByItemIdAndSessionId(itemId, sessionId);
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Товар с ID " + itemId + " не найден"));

        switch (action.toUpperCase()) {
            case "PLUS":
                if (existingCartItem.isPresent()) {
                    CartItem cartItem = existingCartItem.get();
                    if (cartItem.getQuantity() >= item.getStock()) {
                        throw new IllegalStateException("Недостаточно товара на складе. Доступно: " + item.getStock());
                    }
                    cartItem.setQuantity(cartItem.getQuantity() + 1);
                    cartItemRepository.save(cartItem);
                } else {
                    if (item.getStock() < 1) {
                        throw new IllegalStateException("Товар отсутствует на складе");
                    }
                    CartItem newCartItem = new CartItem();
                    newCartItem.setItem(item);
                    newCartItem.setQuantity(1);

                    newCartItem.setSessionId(sessionId);

                    if (currentUser != null) {
                        newCartItem.setUser(currentUser);
                    }

                    cartItemRepository.save(newCartItem);
                    log.info("Created new cart item for user: {}, itemId: {}", currentUser != null ? currentUser.getUsername() : "anonymous", itemId);
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
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            return cartItemRepository.findByItemIdAndUser(itemId, currentUser)
                    .map(CartItem::getQuantity)
                    .orElse(0);
        } else {
            return cartItemRepository.findByItemIdAndSessionId(itemId, sessionId)
                    .map(CartItem::getQuantity)
                    .orElse(0);
        }
    }

    @Transactional
    public void clearCart(String sessionId) {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            cartItemRepository.deleteByUser(currentUser);
        } else {
            cartItemRepository.deleteBySessionId(sessionId);
        }
    }

    @Transactional
    public void migrateAnonymousCartToUser(String sessionId, User user) {
        List<CartItem> anonymousCartItems = cartItemRepository.findBySessionId(sessionId);
        for (CartItem item : anonymousCartItems) {
            Optional<CartItem> existingUserItem = cartItemRepository.findByItemIdAndUser(item.getItem().getId(), user);
            if (existingUserItem.isPresent()) {
                // Если товар уже есть в корзине пользователя, увеличиваем количество
                CartItem userItem = existingUserItem.get();
                userItem.setQuantity(userItem.getQuantity() + item.getQuantity());
                cartItemRepository.save(userItem);
                cartItemRepository.delete(item);
            } else {
                // Иначе просто переносим товар пользователю
                item.setUser(user);
                item.setSessionId(null);
                cartItemRepository.save(item);
            }
        }
    }


}