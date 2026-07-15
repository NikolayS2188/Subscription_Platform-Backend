package com.platform.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Бизнес-исключение для обработки ошибок для ситуаций,
 * которые не являются системными ошибками,
 * но требуют уведомления клиента
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * HTTP статус для ответа
     * По умолчанию UNPROCESSABLE_ENTITY (422)
     */
    private final HttpStatus status;

    /**
     * Дополнительный код ошибки для клиента
     * Для категоризации ошибок
     */
    private final String errorCode;

    /**
     * Конструктор с сообщением
     * Использует статус по умолчанию
     */
    public BusinessException(String message) {
        this(message, HttpStatus.UNPROCESSABLE_ENTITY, null);
    }

    /**
     * Конструктор с сообщением и статусом
     */
    public BusinessException(String message, HttpStatus status) {
        this(message, status, null);
    }

    /**
     * Полный конструктор
     */
    public BusinessException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status != null ? status : HttpStatus.UNPROCESSABLE_ENTITY;
        this.errorCode = errorCode;
    }
}
