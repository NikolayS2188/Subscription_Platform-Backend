package com.platform.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Запрос на оплату обязательства
 * Все поля опциональны - если не указаны, используются значения из обязательства
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    /**
     * Фактически уплаченная сумма
     * Если не указана - берется сумма из обязательства
     * Может отличаться от суммы обязательства (например, частичная оплата)
     */
    private BigDecimal amount;

    /**
     * Валюта платежа
     * Если не указана - берется валюта обязательства
     * Должна соответствовать ISO 4217 (RUB, USD, EUR)
     */
    private String currency;
}
