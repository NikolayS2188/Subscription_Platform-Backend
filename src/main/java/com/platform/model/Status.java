package com.platform.model;

/**
 * Статус обязательства
 * Определяет текущее состояние обязательства
 */
public enum Status {
    /**
     * Активное - обязательство в силе
     * Для рекуррентных: можно оплачивать
     * Для разовых: можно оплатить до даты истечения
     */
    ACTIVE("active", "Активно"),

    /**
     * Отменено - обязательство прекращено
     * Устанавливается:
     * - Для разовых после оплаты
     * - Вручную пользователем
     */
    CANCELLED("cancelled", "Отменено"),

    /**
     * Просрочено - срок истек
     * Устанавливается автоматически при Lazy Expiry
     * Только для разовых платежей!
     */
    EXPIRED("expired", "Просрочено");

    private final String code;
    private final String displayName;

    Status(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Получить статус по коду
     */
    public static Status fromCode(String code) {
        for (Status status : Status.values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Проверка, можно ли выполнить оплату
     */
    public boolean isPayable() {
        return this == ACTIVE;
    }

    /**
     * Проверка, является ли статус терминальным (конечным)
     */
    public boolean isTerminal() {
        return this == CANCELLED || this == EXPIRED;
    }
}
