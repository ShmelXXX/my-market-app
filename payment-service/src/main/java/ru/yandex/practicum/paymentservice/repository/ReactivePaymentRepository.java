package ru.yandex.practicum.paymentservice.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.paymentservice.model.PaymentEntity;

import java.util.UUID;

@Repository
public interface ReactivePaymentRepository extends ReactiveCrudRepository<PaymentEntity, UUID> {

    Flux<PaymentEntity> findByUserId(String userId);

    Flux<PaymentEntity> findByOrderId(String orderId);

    Mono<Long> countByUserIdAndStatus(String userId, String status);
}