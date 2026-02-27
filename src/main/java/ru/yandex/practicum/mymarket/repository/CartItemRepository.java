package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.CartItem;

@Repository
public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {

    Flux<CartItem> findBySessionId(String sessionId);

    Mono<CartItem> findBySessionIdAndItemId(String sessionId, Long itemId);

    @Modifying
    @Query("DELETE FROM cart_items WHERE session_id = :sessionId")
    Mono<Void> deleteBySessionId(String sessionId);

//    @Modifying
//    @Query("DELETE FROM cart_items WHERE session_id = :sessionId AND item_id = :itemId")
//    Mono<Void> deleteBySessionIdAndItemId(String sessionId, Long itemId);
}
