package ru.yandex.practicum.payment.api;

import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class ApiUtil {


    // Возвращает пример ответа для реактивного API
    public static Mono<Void> getExampleResponse(ServerWebExchange exchange,
                                                MediaType mediaType,
                                                String example) {
        exchange.getResponse().getHeaders().setContentType(mediaType);
        byte[] bytes = example.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }


    // @deprecated Старый метод для синхронного API, оставлен для совместимости
    @Deprecated
    public static void setExampleResponse(org.springframework.web.context.request.NativeWebRequest request,
                                          String contentType,
                                          String example) {
        try {
            jakarta.servlet.http.HttpServletResponse response =
                    request.getNativeResponse(jakarta.servlet.http.HttpServletResponse.class);
            assert response != null;
            response.setCharacterEncoding("UTF-8");
            response.addHeader("Content-Type", contentType);
            response.getWriter().print(example);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}