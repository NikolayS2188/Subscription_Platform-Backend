package com.platform.model.dto;

import com.platform.model.Obligation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ при создании обязательства
 * Содержит созданное обязательство и/или предупреждение о дупликате
 */
@Data
@Builder
public class ObligationResponse {

    /**
     * Созданное обязательство
     * Всегда присутствует в ответе
     */
    private Obligation obligation;

    /**
     * Предупреждение о дубликате
     * Присутствует только если найдено активное обязательство с таким же названием
     * Не является ошибкой - просто информирует клиента
     */
    private String warning;
}
