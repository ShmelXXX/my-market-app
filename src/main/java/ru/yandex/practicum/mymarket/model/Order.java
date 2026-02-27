package ru.yandex.practicum.mymarket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("ORDERS")
public class Order {
    @Id
    @Column("ID")
    private Long id;

    @Column("ORDER_DATE")
    private LocalDateTime orderDate;

    @Column("TOTAL_SUM")
    private Long totalSum;
}