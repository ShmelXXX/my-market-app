package ru.yandex.practicum.mymarket.dto;

public record ItemDto(
        Long id,
        String title,
        String description,
        String imgPath,
        Long price,
        Integer count
) {
}