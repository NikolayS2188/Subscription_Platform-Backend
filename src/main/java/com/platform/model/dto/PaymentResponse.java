package com.platform.model.dto;

import com.platform.model.Obligation;
import com.platform.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ на оплату обязательства
 * Содержит обновленное обязательство и созданную запись платежа
 */
@Data
@Builder
public class PaymentResponse {

    /**
     * Обновленное обязательство после оплаты
     * - Для рекуррентных: сдвинута дата следующего платежа
     * - Для разовых: статус изменен на CANCELLED
     */
    private Obligation obligation;

    /**
     * Созданная запись платежа
     * Содержит сумму, валюту и время оплаты
     */
    private Payment payment;
}
