package ru.yandex.practicum.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.PaymentsApi;
import ru.yandex.practicum.payment.model.*;
import ru.yandex.practicum.paymentservice.service.ReactivePaymentService;
import jakarta.validation.Valid;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentsApi {

    private final ReactivePaymentService paymentService;

    @Override
    public Mono<ResponseEntity<PaymentResponse>> createPayment(
            @Valid Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange) {

        log.info("Получен запрос на создание платежа");

        return paymentRequest
                .flatMap(paymentService::createPayment)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Ошибка создания платежа", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Override
    public Mono<ResponseEntity<PaymentDetails>> getPaymentById(
            UUID paymentId,
            ServerWebExchange exchange) {

        log.info("Получен запрос на получение платежа: {}", paymentId);

        return paymentService.getPaymentById(paymentId)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Ошибка получения платежа", e);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @Override
    public Mono<ResponseEntity<RefundResponse>> refundPayment(
            UUID paymentId,
            @Valid Mono<RefundRequest> refundRequest,
            ServerWebExchange exchange) {

        log.info("Получен запрос на возврат платежа: {}", paymentId);

        return refundRequest
                .flatMap(request -> paymentService.refundPayment(paymentId, request))
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Ошибка возврата платежа", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/balance/{userId}")
    public Mono<ResponseEntity<BalanceResponse>> getUserBalance(@PathVariable String userId) {
        log.info("Получен запрос на получение баланса пользователя: {}", userId);
        return paymentService.getUserBalance(userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    log.error("Ошибка получения баланса для пользователя {}", userId, e);

                    BalanceResponse fallback = new BalanceResponse(
                            userId,
                            0,
                            BalanceResponse.CurrencyEnum.RUB,
                            BalanceResponse.StatusEnum.SERVICE_DOWN
                    );
                    fallback.setLastUpdated(OffsetDateTime.now(ZoneOffset.UTC));

                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(fallback));
                });
    }

    @PostMapping("/api/v1/payments")
    public Mono<ResponseEntity<PaymentResponse>> createPayment(@RequestBody PaymentRequest request) {
        log.info("Получен запрос на создание платежа: {}", request);
        return paymentService.createPayment(request)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Ошибка создания платежа", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}