package com.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурация Swagger/OpenAPI для автоматической генерации документации
 * Доступна по адресу: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Subscription Platform API")
                        .description("""
                                API для управления подписками и регулярными платежами
                                
                                ## Бизнес-правила
                                - Lazy Expiry: только разовые платежи автоматически становятся EXPIRED
                                - Рекуррентные подписки остаются ACTIVE даже при просрочке
                                - Дубли не блокируют создание, а возвращают warning
                                - Расчет дат с учетом календарных особенностей (31.01 + 1 месяц = 28.02)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("NikolayS2188")
                                .url("https://github.com/NikolayS2188"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.subscription-platform.com")
                                .description("Production Server")
                ));
    }
}
