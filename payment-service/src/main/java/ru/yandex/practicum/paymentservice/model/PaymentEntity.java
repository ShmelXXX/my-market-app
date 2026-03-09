package ru.yandex.practicum.paymentservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import ru.yandex.practicum.payment.model.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@Table("payments")
public class PaymentEntity {

    @Id
    @Column("id")
    private UUID id;

    @Column("order_id")
    private String orderId;

    @Column("user_id")
    private String userId;

    @Column("amount")
    private Long amount;

    @Column("currency")
    private String currency;

    @Column("status")
    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Column("processed_at")
    private Instant processedAt;

    @Column("payment_method")
    private String paymentMethod;

    @Column("return_url")
    private String returnUrl;

    public PaymentStatus getPaymentStatus() {
        return status != null ? PaymentStatus.fromValue(status) : null;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.status = paymentStatus != null ? paymentStatus.getValue() : null;
    }
}