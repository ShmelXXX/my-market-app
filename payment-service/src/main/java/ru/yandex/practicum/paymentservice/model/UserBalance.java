package ru.yandex.practicum.paymentservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@Table("user_balances")
public class UserBalance {

    @Id
    @Column("id")
    private Long id;

    @Column("user_id")
    private String userId;

    @Column("balance")
    private Long balance;

    @Column("currency")
    private String currency;

    @Column("updated_at")
    private Instant updatedAt;
}