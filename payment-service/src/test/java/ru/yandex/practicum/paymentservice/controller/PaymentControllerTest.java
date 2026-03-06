package ru.yandex.practicum.paymentservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.model.*;
import ru.yandex.practicum.paymentservice.service.ReactivePaymentService;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для PaymentController")
class PaymentControllerTest {

    @Mock
    private ReactivePaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private PaymentRequest validRequest;
    private PaymentResponse successResponse;
    private PaymentDetails paymentDetails;
    private BalanceResponse balanceResponse;
    private final String TEST_USER_ID = "user1";
    private final UUID TEST_PAYMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        validRequest = new PaymentRequest();
        validRequest.setOrderId("order-123");
        validRequest.setAmount(10000);
        validRequest.setCurrency(PaymentRequest.CurrencyEnum.RUB);
        validRequest.setPaymentMethod(PaymentRequest.PaymentMethodEnum.CARD);
        validRequest.setDescription("Тестовый платеж");
        validRequest.setReturnUrl(new URI("http://localhost:8080/orders/123"));

        successResponse = new PaymentResponse();
        successResponse.setPaymentId(TEST_PAYMENT_ID);
        successResponse.setStatus(PaymentStatus.SUCCEEDED);
        successResponse.setAmount(10000);
        successResponse.setCreatedAt(OffsetDateTime.now());

        paymentDetails = new PaymentDetails();
        paymentDetails.setPaymentId(TEST_PAYMENT_ID);
        paymentDetails.setOrderId("order-123");
        paymentDetails.setStatus(PaymentStatus.SUCCEEDED);
        paymentDetails.setAmount(10000);

        balanceResponse = new BalanceResponse(
                TEST_USER_ID,
                100000,
                BalanceResponse.CurrencyEnum.RUB,
                BalanceResponse.StatusEnum.AVAILABLE
        );
        balanceResponse.setLastUpdated(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Должен успешно создать платеж")
    void shouldCreatePaymentSuccessfully() {
        when(paymentService.createPayment(any(PaymentRequest.class)))
                .thenReturn(Mono.just(successResponse));

        Mono<ResponseEntity<PaymentResponse>> result = paymentController.createPayment(
                Mono.just(validRequest),
                MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/payments"))
        );

        StepVerifier.create(result)
                .assertNext(responseEntity -> {
                    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(responseEntity.getBody()).isNotNull();
                    assertThat(responseEntity.getBody().getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
                    assertThat(responseEntity.getBody().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен получить платеж по ID")
    void shouldGetPaymentById() {
        when(paymentService.getPaymentById(TEST_PAYMENT_ID))
                .thenReturn(Mono.just(paymentDetails));

        Mono<ResponseEntity<PaymentDetails>> result = paymentController.getPaymentById(
                TEST_PAYMENT_ID,
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/payments/" + TEST_PAYMENT_ID))
        );

        StepVerifier.create(result)
                .assertNext(responseEntity -> {
                    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(responseEntity.getBody()).isNotNull();
                    assertThat(responseEntity.getBody().getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен вернуть 404 при поиске несуществующего платежа")
    void shouldReturnNotFoundWhenPaymentDoesNotExist() {
        when(paymentService.getPaymentById(TEST_PAYMENT_ID))
                .thenReturn(Mono.error(new RuntimeException("Платеж не найден")));

        Mono<ResponseEntity<PaymentDetails>> result = paymentController.getPaymentById(
                TEST_PAYMENT_ID,
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/payments/" + TEST_PAYMENT_ID))
        );

        StepVerifier.create(result)
                .assertNext(responseEntity -> {
                    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Должен получить баланс пользователя")
    void shouldGetUserBalance() {
        when(paymentService.getUserBalance(TEST_USER_ID))
                .thenReturn(Mono.just(balanceResponse));

        Mono<ResponseEntity<BalanceResponse>> result = paymentController.getUserBalance(TEST_USER_ID);

        StepVerifier.create(result)
                .assertNext(responseEntity -> {
                    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(responseEntity.getBody()).isNotNull();
                    assertThat(responseEntity.getBody().getUserId()).isEqualTo(TEST_USER_ID);
                    assertThat(responseEntity.getBody().getAmount()).isEqualTo(100000);
                })
                .verifyComplete();
    }
}