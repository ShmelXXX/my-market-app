package ru.yandex.practicum.mymarket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("CART_ITEMS")
public class CartItem {
    @Id
    @Column("ID")
    private Long id;

    @Column("ITEM_ID")
    private Long itemId;

    @Column("QUANTITY")
    private Integer quantity;

    @Column("SESSION_ID")
    private String sessionId;

    @Transient
    private Item item;
}