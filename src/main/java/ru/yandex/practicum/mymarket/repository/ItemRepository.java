package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.Item;

@Repository
public interface ItemRepository extends R2dbcRepository<Item, Long> {

    // Поиск с пагинацией
    @Query("SELECT * FROM items WHERE " +
            "LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "LIMIT :size OFFSET :offset")
    Flux<Item> searchItems(@Param("search") String search,
                           @Param("size") int size,
                           @Param("offset") int offset);

    // Поиск с сортировкой по названию
    @Query("SELECT * FROM items WHERE " +
            "LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "ORDER BY title ASC LIMIT :size OFFSET :offset")
    Flux<Item> searchItemsOrderByTitle(@Param("search") String search,
                                       @Param("size") int size,
                                       @Param("offset") int offset);

    // Поиск с сортировкой по цене
    @Query("SELECT * FROM items WHERE " +
            "LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "ORDER BY price ASC LIMIT :size OFFSET :offset")
    Flux<Item> searchItemsOrderByPrice(@Param("search") String search,
                                       @Param("size") int size,
                                       @Param("offset") int offset);

    // Подсчет результатов поиска
    @Query("SELECT COUNT(*) FROM items WHERE " +
            "LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Mono<Long> countSearchItems(@Param("search") String search);

    // Все товары с пагинацией
    @Query("SELECT * FROM items LIMIT :size OFFSET :offset")
    Flux<Item> findAllWithPagination(@Param("size") int size,
                                     @Param("offset") int offset);

    // Все товары с сортировкой по названию
    @Query("SELECT * FROM items ORDER BY title ASC LIMIT :size OFFSET :offset")
    Flux<Item> findAllOrderByTitle(@Param("size") int size,
                                   @Param("offset") int offset);

    // Все товары с сортировкой по цене
    @Query("SELECT * FROM items ORDER BY price ASC LIMIT :size OFFSET :offset")
    Flux<Item> findAllOrderByPrice(@Param("size") int size,
                                   @Param("offset") int offset);

    // Подсчет всех товаров
    @Query("SELECT COUNT(*) FROM items")
    Mono<Long> countAllItems();

    // Товары в наличии
    @Query("SELECT * FROM items WHERE stock > 0")
    Flux<Item> findByStockGreaterThan(int stock);
}