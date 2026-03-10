package ru.yandex.practicum.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.yandex.practicum.payment.model.*;
import ru.yandex.practicum.paymentservice.model.PaymentEntity;
import ru.yandex.practicum.paymentservice.model.UserBalance;
import ru.yandex.practicum.paymentservice.repository.ReactivePaymentRepository;
import ru.yandex.practicum.paymentservice.repository.UserBalanceRepository;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactivePaymentService {

    private final ReactivePaymentRepository paymentRepository;
    private final UserBalanceRepository userBalanceRepository;

    // Хранилище балансов пользователей (в реальном проекте - БД)
//    private final Map<String, Long> userBalances = new ConcurrentHashMap<>();
//
//    {
//        // Тестовые данные (суммы в копейках)
//        userBalances.put("user1", 100000L);  // 1000.00 руб
//        userBalances.put("user2", 50000L);
//        userBalances.put("user3", 1000L);
//        userBalances.put("user4", 0L);
//        userBalances.put("user_123", 500000L);
//    }



    @Transactional
    public Mono<PaymentResponse> createPayment(PaymentRequest request) {
        return Mono.fromCallable(() -> {
            String userId = extractUserIdFromRequest(request);

            // Получаем баланс пользователя из БД
            UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                    .blockOptional()
                    .orElseGet(() -> {
                        // Если пользователя нет в БД, создаем запись с нулевым балансом
                        UserBalance newBalance = new UserBalance();
                        newBalance.setUserId(userId);
                        newBalance.setBalance(0L);
                        newBalance.setCurrency("RUB");
                        newBalance.setUpdatedAt(Instant.now());
                        return userBalanceRepository.save(newBalance).block();
                    });

            Long currentBalance = userBalance.getBalance();
            Integer requestedAmount = request.getAmount();

            PaymentResponse response = new PaymentResponse();
            response.setPaymentId(UUID.randomUUID());
            response.setAmount(requestedAmount);
            response.setCreatedAt(OffsetDateTime.now());

            if (currentBalance >= requestedAmount) {
                // Достаточно средств
                userBalance.setBalance(currentBalance - requestedAmount);
                userBalance.setUpdatedAt(Instant.now());
                userBalanceRepository.save(userBalance).block();

                PaymentEntity payment = new PaymentEntity();
                payment.setId(response.getPaymentId());
                payment.setOrderId(request.getOrderId());
                payment.setUserId(userId);
                payment.setAmount(requestedAmount.longValue());
                payment.setCurrency(request.getCurrency().getValue());
                payment.setPaymentStatus(PaymentStatus.SUCCEEDED);
                payment.setCreatedAt(Instant.now());

                if (request.getReturnUrl() != null) {
                    payment.setReturnUrl(request.getReturnUrl().toString());
                }

                paymentRepository.save(payment).block();

                response.setStatus(PaymentStatus.SUCCEEDED);
                response.setPaymentUrl(createPaymentUrl(payment.getId()));

                log.info("Payment successful for user {}: orderId={}, amount={}",
                        userId, request.getOrderId(), requestedAmount);
            } else {
                response.setStatus(PaymentStatus.FAILED);
                response.setErrorMessage(
                        String.format("Insufficient funds. Available: %d, Required: %d",
                                currentBalance, requestedAmount)
                );
                log.warn("Payment failed for user {}: insufficient funds (available: {}, required: {})",
                        userId, currentBalance, requestedAmount);
            }

            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String extractUserIdFromRequest(PaymentRequest request) {
        // 1. Из metadata
        if (request.getMetadata() != null && request.getMetadata().containsKey("userId")) {
            return request.getMetadata().get("userId");
        }

        // 2. Из customerDetails
        if (request.getCustomerDetails() != null && request.getCustomerDetails().getUserId() != null) {
            return request.getCustomerDetails().getUserId();
        }

        // 3. Из description (формат: "...|userId=xxx|...")
        if (request.getDescription() != null && request.getDescription().contains("|userId=")) {
            String desc = request.getDescription();
            int start = desc.indexOf("|userId=") + 8;
            int end = desc.indexOf("|", start);
            if (end == -1) end = desc.length();
            return desc.substring(start, end);
        }

        // 4. По умолчанию
        log.warn("No userId found in request, using default: user1");
        return "user1";
    }

    private URI createPaymentUrl(UUID paymentId) {
        try {
            return new URI("http://localhost:8081/api/v1/payments/" + paymentId);
        } catch (Exception e) {
            log.error("Error creating payment URL", e);
            return null;
        }
    }

    public Mono<PaymentDetails> getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(entity -> {
                    PaymentDetails details = new PaymentDetails();
                    details.setPaymentId(entity.getId());
                    details.setStatus(entity.getPaymentStatus());
                    details.setOrderId(entity.getOrderId());
                    details.setAmount(entity.getAmount().intValue());
                    details.setPaymentMethod(entity.getPaymentMethod());

                    if (entity.getCreatedAt() != null) {
                        details.setCreatedAt(OffsetDateTime.ofInstant(
                                entity.getCreatedAt(), ZoneOffset.UTC));
                    }

                    try {
                        if (entity.getReturnUrl() != null) {
                            details.setPaymentUrl(new URI(entity.getReturnUrl()));
                        }
                    } catch (Exception e) {
                        log.error("Ошибка создания URI", e);
                    }

                    return details;
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Платеж не найден: " + paymentId)));
    }

    public Mono<RefundResponse> refundPayment(UUID paymentId, RefundRequest request) {
        return paymentRepository.findById(paymentId)
                .flatMap(entity -> {
                    if (entity.getPaymentStatus() != PaymentStatus.SUCCEEDED) {
                        return Mono.error(new IllegalStateException(
                                "Невозможно выполнить возврат для платежа со статусом: " + entity.getStatus()));
                    }

                    RefundResponse response = new RefundResponse();
                    response.setRefundId(UUID.randomUUID());
                    response.setPaymentId(paymentId);

                    Long entityAmount = entity.getAmount();
                    Integer requestAmount = request.getAmount();

                    long refundAmount = requestAmount != null ? requestAmount.longValue() : entityAmount;
                    response.setAmount((int) refundAmount);
                    response.setStatus(RefundResponse.StatusEnum.COMPLETED);
                    response.setCreatedAt(OffsetDateTime.now());

                    if (requestAmount != null && requestAmount < entityAmount) {
                        entity.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
                    } else {
                        entity.setPaymentStatus(PaymentStatus.REFUNDED);
                    }

                    return paymentRepository.save(entity).thenReturn(response);
                });
    }

    public Mono<BalanceResponse> getUserBalance(String userId) {
        return userBalanceRepository.findByUserId(userId)
                .map(userBalance -> new BalanceResponse(
                        userId,
                        userBalance.getBalance().intValue(),
                        BalanceResponse.CurrencyEnum.valueOf(userBalance.getCurrency()),
                        BalanceResponse.StatusEnum.AVAILABLE
                ))
                .switchIfEmpty(Mono.fromCallable(() -> {
                    // Если пользователь не найден, создаем запись с нулевым балансом
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUserId(userId);
                    newBalance.setBalance(0L);
                    newBalance.setCurrency("RUB");
                    newBalance.setUpdatedAt(Instant.now());

                    UserBalance saved = userBalanceRepository.save(newBalance).block();

                    return new BalanceResponse(
                            userId,
                            saved.getBalance().intValue(),
                            BalanceResponse.CurrencyEnum.RUB,
                            BalanceResponse.StatusEnum.AVAILABLE
                    );
                }));
    }
}