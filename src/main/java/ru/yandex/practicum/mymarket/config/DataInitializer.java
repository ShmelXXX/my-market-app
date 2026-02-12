package ru.yandex.practicum.mymarket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    @Bean
    public CommandLineRunner initData(ItemRepository itemRepository) {
        return args -> {
            // Добавляем тестовые товары
            Item item1 = new Item();
            item1.setTitle("Футбольный мяч");
            item1.setDescription("Качественный футбольный мяч");
            item1.setImgPath("images/soccer_ball.png");
            item1.setPrice(2500L);
            item1.setStock(20);

            Item item2 = new Item();
            item2.setTitle("Бейсболка красная");
            item2.setDescription("Красивая красная бейсболка");
            item2.setImgPath("images/baseball_cap_red.png");
            item2.setPrice(1000L);
            item2.setStock(1);

            Item item3 = new Item();
            item3.setTitle("Бейсболка черная");
            item3.setDescription("Стильная черная бейсболка");
            item3.setImgPath("images/baseball_cap_black.png");
            item3.setPrice(1500L);
            item3.setStock(2);

            Item item4 = new Item();
            item4.setTitle("Зонт");
            item4.setDescription("супер зонт");
            item4.setImgPath("images/umbrella.png");
            item4.setPrice(1500L);
            item4.setStock(30);

            Item item5 = new Item();
            item5.setTitle("Самолет");
            item5.setDescription("Реактивный самолет");
            item5.setImgPath("images/airplane.png");
            item5.setPrice(1000000000L);
            item5.setStock(0);

            itemRepository.save(item1);
            itemRepository.save(item2);
            itemRepository.save(item3);
            itemRepository.save(item4);
            itemRepository.save(item5);
        };
    }
}