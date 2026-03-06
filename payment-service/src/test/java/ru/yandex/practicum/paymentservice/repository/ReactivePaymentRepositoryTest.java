package ru.yandex.practicum.paymentservice.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import reactor.test.StepVerifier;
import ru.yandex.practicum.paymentservice.model.PaymentEntity;
import ru.yandex.practicum.payment.model.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@DisplayName("Интеграционные тесты для ReactivePaymentRepository")
class ReactivePaymentRepositoryTest {

    @Autowired
    private ReactivePaymentRepository paymentRepository;

    @Test
    @DisplayName("Должен сохранить и найти платеж по ID")
    void shouldSaveAndFindById() {
        PaymentEntity testPayment = createTestPayment();

        paymentRepository.save(testPayment)
                .flatMap(saved -> {
                    UUID generatedId = saved.getId();
                    return paymentRepository.findById(generatedId)
                            .map(found -> {
                                assertThat(found.getId()).isNotNull();
                                assertThat(found.getId()).isEqualTo(generatedId);
                                return found;
                            });
                })
                .as(StepVerifier::create)
                .assertNext(found -> {
                    // Используем testPayment для сравнения
                    assertThat(found.getOrderId()).isEqualTo(testPayment.getOrderId());
                    assertThat(found.getUserId()).isEqualTo(testPayment.getUserId());
                    assertThat(found.getAmount()).isEqualTo(10000L);
                    assertThat(found.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен найти платежи по userId")
    void shouldFindByUserId() {
        PaymentEntity testPayment = createTestPayment();

        paymentRepository.save(testPayment)
                .thenMany(paymentRepository.findByUserId(testPayment.getUserId())) // Исправлено
                .as(StepVerifier::create)
                .assertNext(payment -> {
                    assertThat(payment.getUserId()).isEqualTo(testPayment.getUserId());
                    assertThat(payment.getOrderId()).isEqualTo(testPayment.getOrderId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен найти платежи по orderId")
    void shouldFindByOrderId() {
        PaymentEntity testPayment = createTestPayment();

        paymentRepository.save(testPayment)
                .thenMany(paymentRepository.findByOrderId(testPayment.getOrderId())) // Исправлено
                .as(StepVerifier::create)
                .assertNext(payment -> {
                    assertThat(payment.getOrderId()).isEqualTo(testPayment.getOrderId());
                    assertThat(payment.getUserId()).isEqualTo(testPayment.getUserId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен посчитать количество платежей по статусу")
    void shouldCountByUserIdAndStatus() {
        PaymentEntity testPayment = createTestPayment();

        paymentRepository.save(testPayment)
                .flatMap(saved ->
                        paymentRepository.countByUserIdAndStatus(
                                saved.getUserId(),      // Исправлено
                                saved.getStatus()        // Исправлено
                        )
                )
                .as(StepVerifier::create)
                .assertNext(count -> assertThat(count).isEqualTo(1L))
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен удалить платеж")
    void shouldDeletePayment() {
        PaymentEntity testPayment = createTestPayment();

        paymentRepository.save(testPayment)
                .flatMap(saved -> paymentRepository.delete(saved)
                        .thenReturn(saved.getId()))
                .flatMapMany(id -> paymentRepository.findById(id))
                .as(StepVerifier::create)
                .verifyComplete();
    }

    private PaymentEntity createTestPayment() {
        PaymentEntity payment = new PaymentEntity();
        String uniqueId = UUID.randomUUID().toString();

        payment.setOrderId("order-" + uniqueId);
        payment.setUserId("user-" + uniqueId);
        payment.setAmount(10000L);
        payment.setCurrency("RUB");
        payment.setStatus(PaymentStatus.SUCCEEDED.toString());
        payment.setCreatedAt(Instant.now());
        payment.setPaymentMethod("CARD");
        payment.setReturnUrl("http://localhost:8080/orders/" + uniqueId);
        return payment;
    }
}