-- ============================================
-- V1__Initial_schema.sql
-- Инициализация схемы базы данных
-- ============================================

-- 1. Создание таблицы обязательств
CREATE TABLE IF NOT EXISTS obligations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    category VARCHAR(20) NOT NULL,
    recurrence VARCHAR(20),
    next_payment_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- 2. Создание таблицы истории оплат
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    obligation_id UUID NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_obligation
    FOREIGN KEY (obligation_id)
    REFERENCES obligations(id)
    ON DELETE CASCADE
    );

-- 3. Индексы для производительности
-- Индекс для поиска по статусу
CREATE INDEX IF NOT EXISTS idx_obligations_status
    ON obligations(status);

-- Индекс для поиска по категории
CREATE INDEX IF NOT EXISTS idx_obligations_category
    ON obligations(category);

-- Индекс для поиска по дате платежа
CREATE INDEX IF NOT EXISTS idx_obligations_next_payment_date
    ON obligations(next_payment_date);

-- Составной индекс для поиска по названию и статусу (регистронезависимый)
CREATE INDEX IF NOT EXISTS idx_obligations_title_status
    ON obligations(LOWER(title), status);

-- Индекс для внешнего ключа в таблице платежей
CREATE INDEX IF NOT EXISTS idx_payments_obligation_id
    ON payments(obligation_id);

-- Индекс для поиска по дате оплаты
CREATE INDEX IF NOT EXISTS idx_payments_paid_at
    ON payments(paid_at);

-- 4. Функция для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- 5. Триггер для автоматического обновления updated_at
DROP TRIGGER IF EXISTS update_obligations_updated_at ON obligations;
CREATE TRIGGER update_obligations_updated_at
    BEFORE UPDATE ON obligations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 6. Комментарии к таблицам и колонкам
COMMENT ON TABLE obligations IS 'Таблица обязательств (подписки, счета, страховки, гарантии)';
COMMENT ON COLUMN obligations.id IS 'Уникальный идентификатор (UUID)';
COMMENT ON COLUMN obligations.title IS 'Название обязательства';
COMMENT ON COLUMN obligations.amount IS 'Сумма платежа';
COMMENT ON COLUMN obligations.currency IS 'Валюта (ISO 4217: RUB, USD, EUR)';
COMMENT ON COLUMN obligations.category IS 'Категория: subscription, warranty, bill, insurance';
COMMENT ON COLUMN obligations.recurrence IS 'Периодичность: monthly, quarterly, yearly, null - разовое';
COMMENT ON COLUMN obligations.next_payment_date IS 'Дата следующего платежа или истечения срока';
COMMENT ON COLUMN obligations.status IS 'Статус: active, cancelled, expired';
COMMENT ON COLUMN obligations.created_at IS 'Дата создания записи';
COMMENT ON COLUMN obligations.updated_at IS 'Дата последнего обновления';

COMMENT ON TABLE payments IS 'История оплат обязательств';
COMMENT ON COLUMN payments.id IS 'Уникальный идентификатор (UUID)';
COMMENT ON COLUMN payments.obligation_id IS 'Ссылка на обязательство';
COMMENT ON COLUMN payments.amount IS 'Фактически уплаченная сумма';
COMMENT ON COLUMN payments.currency IS 'Валюта платежа';
COMMENT ON COLUMN payments.paid_at IS 'Момент фиксации оплаты';

-- 7. Проверка целостности (констрейнты)
-- Статус не может быть NULL
ALTER TABLE obligations ALTER COLUMN status SET NOT NULL;

-- Категория не может быть NULL
ALTER TABLE obligations ALTER COLUMN category SET NOT NULL;

-- Добавляем CHECK констрейнты
ALTER TABLE obligations ADD CONSTRAINT chk_currency
    CHECK (currency IN ('RUB', 'USD', 'EUR'));

ALTER TABLE obligations ADD CONSTRAINT chk_category
    CHECK (category IN ('SUBSCRIPTION', 'WARRANTY', 'BILL', 'INSURANCE'));

ALTER TABLE obligations ADD CONSTRAINT chk_recurrence
    CHECK (recurrence IN ('MONTHLY', 'QUARTERLY', 'YEARLY') OR recurrence IS NULL);

ALTER TABLE obligations ADD CONSTRAINT chk_status
    CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED'));

ALTER TABLE payments ADD CONSTRAINT chk_payment_currency
    CHECK (currency IN ('RUB', 'USD', 'EUR'));

-- 8. Создание последовательности для генерации ID (если не используется UUID)
-- Не требуется, так как используем UUID с gen_random_uuid()