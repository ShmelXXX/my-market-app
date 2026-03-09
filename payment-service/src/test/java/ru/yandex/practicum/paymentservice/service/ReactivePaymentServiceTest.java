package ru.yandex.practicum.paymentservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.model.*;
import ru.yandex.practicum.paymentservice.model.PaymentEntity;
import ru.yandex.practicum.paymentservice.repository.ReactivePaymentRepository;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для ReactivePaymentService")
class ReactivePaymentServiceTest {

    @Mock
    private ReactivePaymentRepository paymentRepository;

    @InjectMocks
    private ReactivePaymentService paymentService;

    private PaymentRequest validRequest;
    private PaymentRequest invalidRequest;
    private PaymentEntity savedEntity;
    private final String TEST_USER_ID = "user1";
    private final String TEST_ORDER_ID = "order-123";
    private final Integer TEST_AMOUNT = 10000; // 100 руб

    @BeforeEach
    void setUp() throws Exception {
        validRequest = new PaymentRequest();
        validRequest.setOrderId(TEST_ORDER_ID);
        validRequest.setAmount(TEST_AMOUNT);
        validRequest.setCurrency(PaymentRequest.CurrencyEnum.RUB);
        validRequest.setPaymentMethod(PaymentRequest.PaymentMethodEnum.CARD);
        validRequest.setDescription("Тестовый платеж|userId=" + TEST_USER_ID);
        validRequest.setReturnUrl(new URI("http://localhost:8080/orders/123"));

        invalidRequest = new PaymentRequest();
        invalidRequest.setOrderId(TEST_ORDER_ID);
        invalidRequest.setAmount(500000); // 5000 руб (больше чем у user1)
        invalidRequest.setCurrency(PaymentRequest.CurrencyEnum.RUB);
        invalidRequest.setPaymentMethod(PaymentRequest.PaymentMethodEnum.CARD);
        invalidRequest.setDescription("Тестовый платеж|userId=" + TEST_USER_ID);

        savedEntity = new PaymentEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setOrderId(TEST_ORDER_ID);
        savedEntity.setUserId(TEST_USER_ID);
        savedEntity.setAmount(TEST_AMOUNT.longValue());
        savedEntity.setCurrency("RUB");
        savedEntity.setStatus(PaymentStatus.SUCCEEDED.toString());
        savedEntity.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Должен успешно создать платеж при достаточном балансе")
    void shouldCreatePaymentSuccessfully() {
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(Mono.just(savedEntity));

        Mono<PaymentResponse> result = paymentService.createPayment(validRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
                    assertThat(response.getAmount()).isEqualTo(TEST_AMOUNT);
                    assertThat(response.getPaymentId()).isNotNull();
                })
                .verifyComplete();

        verify(paymentRepository).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("Должен отклонить платеж при недостаточном балансе")
    void shouldFailPaymentWhenInsufficientFunds() {
        Mono<PaymentResponse> result = paymentService.createPayment(invalidRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
                    assertThat(response.getErrorMessage()).contains("Insufficient funds");
                })
                .verifyComplete();

        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("Должен извлечь userId из description")
    void shouldExtractUserIdFromDescription() {
        PaymentRequest request = new PaymentRequest();
        request.setDescription("Оплата заказа|userId=user123|доп.инфо");

        // Используем рефлексию для вызова приватного метода
        java.lang.reflect.Method method = null;
        try {
            method = ReactivePaymentService.class.getDeclaredMethod("extractUserIdFromRequest", PaymentRequest.class);
            method.setAccessible(true);
            String userId = (String) method.invoke(paymentService, request);
            assertThat(userId).isEqualTo("user123");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Должен получить платеж по ID")
    void shouldGetPaymentById() {
        UUID paymentId = savedEntity.getId();
        when(paymentRepository.findById(paymentId)).thenReturn(Mono.just(savedEntity));

        Mono<PaymentDetails> result = paymentService.getPaymentById(paymentId);

        StepVerifier.create(result)
                .assertNext(details -> {
                    assertThat(details.getPaymentId()).isEqualTo(paymentId);
                    assertThat(details.getOrderId()).isEqualTo(TEST_ORDER_ID);
                    assertThat(details.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен вернуть ошибку при поиске несуществующего платежа")
    void shouldReturnErrorWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Mono.empty());

        Mono<PaymentDetails> result = paymentService.getPaymentById(paymentId);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("Должен получить баланс пользователя")
    void shouldGetUserBalance() {
        String userId = "user1";

        Mono<BalanceResponse> result = paymentService.getUserBalance(userId);

        StepVerifier.create(result)
                .assertNext(balance -> {
                    assertThat(balance.getUserId()).isEqualTo(userId);
                    assertThat(balance.getAmount()).isPositive();
                    assertThat(balance.getCurrency()).isEqualTo(BalanceResponse.CurrencyEnum.RUB);
                    assertThat(balance.getStatus()).isEqualTo(BalanceResponse.StatusEnum.AVAILABLE);
                })
                .verifyComplete();
    }
}