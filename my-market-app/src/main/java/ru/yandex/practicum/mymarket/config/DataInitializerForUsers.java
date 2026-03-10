package ru.yandex.practicum.mymarket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.UserRepository;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializerForUsers {

    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                User user1 = new User();
                user1.setUsername("user1");
                user1.setPassword(passwordEncoder.encode("password"));
                user1.setEmail("user1@example.com");
                user1.setRole("ROLE_USER");

                User user2 = new User();
                user2.setUsername("user2");
                user2.setPassword(passwordEncoder.encode("password2"));
                user2.setEmail("user2@example.com");
                user2.setRole("ROLE_USER");

                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setEmail("admin@example.com");
                admin.setRole("ROLE_ADMIN");

                userRepository.save(user1);
                userRepository.save(user2);
                userRepository.save(admin);

                log.info("Test users created:");
                log.info("user1/password");
                log.info("user2/password2");
                log.info("admin/admin");
            }
        };
    }
}