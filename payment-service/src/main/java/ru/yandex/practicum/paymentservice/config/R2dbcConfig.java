package ru.yandex.practicum.paymentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableR2dbcAuditing
@EnableTransactionManagement
public class R2dbcConfig {
}