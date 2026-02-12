package ru.yandex.practicum.mymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private List<ItemDto> items;
    private Long totalSum;

    // Добавляем методы, которые используются в HTML шаблонах
    public Long id() {
        return id;
    }

    public List<ItemDto> items() {
        return items;
    }

    public Long totalSum() {
        return totalSum;
    }
}