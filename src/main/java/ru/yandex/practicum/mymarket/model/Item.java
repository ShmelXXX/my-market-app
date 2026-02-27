package ru.yandex.practicum.mymarket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("ITEMS")
public class Item {
    @Id
    @Column("ID")
    private Long id;

    @Column("TITLE")
    private String title;

    @Column("DESCRIPTION")
    private String description;

    @Column("IMG_PATH")
    private String imgPath;

    @Column("PRICE")
    private Long price;

    @Column("STOCK")
    private Integer stock;
}