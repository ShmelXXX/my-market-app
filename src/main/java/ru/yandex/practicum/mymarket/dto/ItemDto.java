package ru.yandex.practicum.mymarket.dto;

public record ItemDto(
        Long id,
        String title,
        String description,
        String imgPath,
        Long price,
        Integer count
) {}

//    // Добавляем методы, которые используются в HTML шаблонах
//    public Long id() {
//        return id;
//    }
//
//    public String title() {
//        return title;
//    }
//
//    public String description() {
//        return description;
//    }
//
//    public String imgPath() {
//        return imgPath;
//    }
//
//    public Long price() {
//        return price;
//    }
//
//    public Integer count() {
//        return count;
//    }
//}