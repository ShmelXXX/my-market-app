package ru.yandex.practicum.mymarket.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
public class Item implements Serializable {  // Добавлено implements Serializable{

    private static final long serialVersionUID = 1L;  // Добавлен serialVersionUID

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String imgPath;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Integer stock;
}