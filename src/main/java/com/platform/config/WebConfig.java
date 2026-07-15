package com.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация веб-слоя
 * Настройка CORS для разрешения запросов с разных доменов
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // Применить ко всем эндпоинтам
                .allowedOrigins("*")    // Разрешить запросы с любых доменов (т.к. пока нет конкретных доменов)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // Разрешить эти HTTP-методы
                .allowedHeaders("*")    // Разрешить любые заголовки
                .exposedHeaders("Authorization")    // Указание заголовок бэкенда для фронта
                .allowCredentials(false)    // Не разрешать передачу кук/авторизационных данных (т.к. пока нет конкретных доменов)
                .maxAge(3600); // Кэширование preflight-ответа CORS на 1 час (OPTIONS-запрос будет отправлен только раз в час для экономии трафика и времени)
    }
}
