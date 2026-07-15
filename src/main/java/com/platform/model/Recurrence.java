package com.platform.model;

/**
 * Периодичность повторения обязательства
 * null означает разовое обязательство
 */
public enum Recurrence {
    /**
     * Ежемесячная периодичность
     * Сдвиг даты на 1 месяц
     */
    MONTHLY("monthly", "Ежемесячно", 1),

    /**
     * Ежеквартальная периодичность
     * Сдвиг даты на 3 месяца
     */
    QUARTERLY("quarterly", "Ежеквартально", 3),

    /**
     * Ежегодная периодичность
     * Сдвиг даты на 1 год
     */
    YEARLY("yearly", "Ежегодно", 12);

    private final String code;
    private final String displayName;
    private final int monthsToAdd;

    Recurrence(String code, String displayName, int monthsToAdd) {
        this.code = code;
        this.displayName = displayName;
        this.monthsToAdd = monthsToAdd;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Количество месяцев для сдвига даты
     */
    public int getMonthsToAdd() {
        return monthsToAdd;
    }

    /**
     * Получить периодичность по коду
     */
    public static Recurrence fromCode(String code) {
        for (Recurrence recurrence : Recurrence.values()) {
            if (recurrence.code.equalsIgnoreCase(code)) {
                return recurrence;
            }
        }
        return null;
    }

    /**
     * Проверка, является ли периодичность рекуррентной
     * (всегда true, так как null - это отсутствие периодичности)
     */
    public boolean isRecurring() {
        return true;
    }
}
