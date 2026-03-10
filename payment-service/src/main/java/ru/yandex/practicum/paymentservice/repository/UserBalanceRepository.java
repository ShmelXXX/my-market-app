package ru.yandex.practicum.paymentservice.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.paymentservice.model.UserBalance;

@Repository
public interface UserBalanceRepository extends ReactiveCrudRepository<UserBalance, Long> {

    Mono<UserBalance> findByUserId(String userId);
}