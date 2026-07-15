package com.platform.model;

/**
 * Категории обязательств
 * Определяет тип обязательства для классификации и фильтрации
 */
public enum Category {
    /**
     * Подписка - регулярные сервисы (Netflix, Яндекс.Плюс, Spotify и т.д.)
     */
    SUBSCRIPTION("subscription", "Подписка"),

    /**
     * Гарантия - гарантийные обязательства
     */
    WARRANTY("warranty", "Гарантия"),

    /**
     * Счет - коммунальные платежи, интернет, телефон
     */
    BILL("bill", "Счет"),

    /**
     * Страховка - страховые полисы
     */
    INSURANCE("insurance", "Страховка");

    private final String code;
    private final String displayName;

    Category(String code, String displayName) {
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
     * Получить категорию по коду
     * @param code строковый код категории
     * @return Category или null если не найдено
     */
    public static Category fromCode(String code) {
        for (Category category : Category.values()) {
            if (category.code.equalsIgnoreCase(code)) {
                return category;
            }
        }
        return null;
    }

    /**
     * Проверка валидности категории
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
