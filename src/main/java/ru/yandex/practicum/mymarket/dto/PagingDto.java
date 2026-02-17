package ru.yandex.practicum.mymarket.dto;

public record PagingDto(
    int pageSize,
    int pageNumber,
    boolean hasPrevious,
    boolean hasNext
){}

//    public PagingDto(int pageSize, int pageNumber, boolean b, boolean b1) {
//        this();
//    }

//    // Добавляем методы, которые используются в HTML шаблонах
//    public int pageSize() {
//        return pageSize;
//    }
//
//    public int pageNumber() {
//        return pageNumber;
//    }
//
//    public boolean hasPrevious() {
//        return hasPrevious;
//    }
//
//    public boolean hasNext() {
//        return hasNext;
//    }
//}