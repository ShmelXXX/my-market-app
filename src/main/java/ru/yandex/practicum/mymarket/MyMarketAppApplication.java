package ru.yandex.practicum.mymarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "ru.yandex.practicum.mymarket.repository")
public class MyMarketAppApplication {
	public static void main(String[] args) {
		SpringApplication.run(MyMarketAppApplication.class, args);
	}
}