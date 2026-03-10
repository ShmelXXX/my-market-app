package ru.yandex.practicum.mymarket.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findBySessionId(String sessionId);

    List<CartItem> findByUser(User user);

    Optional<CartItem> findByItemIdAndSessionId(Long itemId, String sessionId);

    Optional<CartItem> findByItemIdAndUser(Long itemId, User user);

    void deleteBySessionId(String sessionId);

    void deleteByUser(User user);
}
