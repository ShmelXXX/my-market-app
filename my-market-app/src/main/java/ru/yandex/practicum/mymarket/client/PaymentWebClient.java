package ru.yandex.practicum.mymarket.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.model.BalanceResponse;

import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentWebClient {

    private final WebClient paymentServiceWebClient;

    public Mono<PaymentResponse> createPayment(PaymentRequest request) {
        return paymentServiceWebClient.post()
                .uri("/api/v1/payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(response -> {
                    assert response != null;
                    log.info("Payment created: {}", response.getPaymentId());
                })
                .doOnError(error -> log.error("Error creating payment", error));
    }

    public Mono<BalanceResponse> getUserBalance(String userId) {
        return paymentServiceWebClient.get()
                .uri("/balance/{userId}", userId)  // Исправлено: /api/v1/balance/{userId}
                .retrieve()
                .bodyToMono(BalanceResponse.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> {
                    log.error("Error getting balance for user {}: {}", userId, e.getMessage());

                    BalanceResponse fallback = new BalanceResponse();
                    fallback.setUserId(userId);
                    fallback.setAmount(0);
                    fallback.setCurrency(BalanceResponse.CurrencyEnum.RUB);
                    fallback.setLastUpdated(OffsetDateTime.now(ZoneOffset.UTC));

                    fallback.setStatus(BalanceResponse.StatusEnum.SERVICE_DOWN);

                    return Mono.just(fallback);
                });
    }
}