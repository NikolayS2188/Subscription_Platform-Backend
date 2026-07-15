# Subscription Platform - Backend

**AI-платформа управления личными подписками и регулярными платежами**

## Содержание

---

- [Описание](#описание)
   - [Ключевые функции](#ключевые-функции)
   - [Бизнес-правила](#бизнес-правила)
      - [Lazy Expiry](#lazy-expiry)
      - [Расчет дат при реккурентности](#расчет-дат-при-реккурентности)
- [Технологии](#технологии)
- [Архитектура](#архитектура)
   - [Слои архитектуры](#слои-архитектуры)
   - [Потоки данных](#потоки-данных)
   - [Связи](#связи)
   - [Таблицы](#таблицы)
   - [Индексы и триггеры](#индексы-и-триггеры)
- [Конфигурация](#конфигурация)
   - [Переменные окружения](#переменные-окружения)
   - [Профили](#профили)
- [Установка](#установка)
   - [Требования](#требования)
   - [Клонирование](#клонирование)
   - [Настройка окружения](#настройка-окружения)
- [Запуск](#запуск)
   - [Через Docker](#через-docker)
   - [Через Maven](#через-maven)
   - [Проверка работоспособности](#проверка-работоспособности)
- [API Документация](#api-документация)
- [API Эндпоинты](#api-эндпоинты)
   - [POST /obligations](#post-obligations)
   - [GET /obligations](#get-obligations)
   - [GET /obligations/upcoming?days=N](#предстоящие-списания)
   - [POST /obligations/{id}/pay](#post-obligationsidpay)
   - [DELETE /obligations/{id}](#delete-obligationsid)
   - [GET /obligations/stream](#get-obligationsstream)
- [Деплой](#деплой)
   - [Docker сборка](#docker-сборка)
   - [Мониторинг](#мониторинг)
- [Очистка](#очистка)
- [Лицензия](#лицензия)


## Описание

---

Платформа «Умный реестр подписок» для автоматизации учёта, расчёта дат списаний и контроля статуса подписок пользователя.

### Ключевые функции

- Создание обязательств (подписки, гарантии, счета, страховки)
- Автоматический расчет дат списаний с учетом календарных особенностей
- Lazy expiry для корректной работы с просрочками
- SSE события для real-time обновлений
- Поддержка разовых и рекуррентных платежей
- AI-интеграция (дубли не прерывают работу)

### Бизнес-правила

1. **Lazy Expiry** — только разовые платежи автоматически становятся EXPIRED
2. **Рекуррентные подписки** остаются ACTIVE даже при просрочке
3. **Дубли** не блокируют создание, а возвращают warning
4. **Расчет дат** с учетом календарных особенностей (31.01 + 1 месяц = 28.02)

#### Lazy Expiry
Обоснование бизнес-правила Lazy Expiry

**Проблема:** Как определить, что подписка действительно просрочена, если платеж может быть произведен с задержкой?

**Решение: Lazy Expiry** - мы не переводим рекуррентные подписки в статус EXPIRED автоматически. Они остаются ACTIVE до ручного перевода в CANCELLED. Только разовые платежи автоматически становятся EXPIRED.

**Почему:**
1. **Сервис продолжает работать** - даже если пользователь забыл оплатить подписку, доступ к сервису сохраняется в течение нескольких дней после просрочки;
2. **Нет автоматического отказа** - система не принимает решение о прекращении подписки за пользователя;
3. **Гибкость оплаты** - пользователь может оплатить с задержкой, и подписка продолжит действовать;
4. **Разовые платежи** - в данном случае выполняется автоматическое истечение EXPIRED сразу, так как услуга была оказана в конкретную дату.

**Реализация:**
```java
// Только разовые платежи (recurrence == null) становятся EXPIRED
if (obligation.getStatus() == Status.ACTIVE &&
        obligation.getRecurrence() == null &&
        obligation.getNextPaymentDate().isBefore(today)) {
        obligation.setStatus(Status.EXPIRED);
}
```

#### Расчет дат при реккурентности
Cдвиг от текущего nextPaymentDate, а не от даты оплаты. Это предотвращает накопление ошибок при просрочках. 

Используется LocalDate.plusMonths():
```text
  Пример                          Результат
31 января + 1 месяц             28/29 февраля
31 марта + 1 месяц              30 апреля
31 декабря + 1 месяц            31 января
```

## Технологии

---

| Технология | Версия | Описание |
|------------|--------|----------|
| **Java** | 21     | Основной язык |
| **Spring Boot** | 3.1.5  | Фреймворк |
| **PostgreSQL** | 15     | База данных |
| **Hibernate** | 6.x    | ORM |
| **Maven** | 3.9+   | Сборка |
| **Flyway** | 9.x    | Миграции БД |
| **Docker** | Latest | Контейнеризация |
| **Lombok** | -      | Генерация кода |
| **Swagger/OpenAPI** | 2.2.0  | Документация API |


## Архитектура
Приложение построено по многослойной архитектуре с четким разделением ответственности.

### Слои архитектуры
1. Верхний уровень — внешние потребители API: 

   • Frontend — веб-интерфейс пользователя 

   • Telegram Bot — бот для уведомлений 

   • AI-агент — автоматическое создание обязательств 

   • Swagger UI — интерактивная документация 


2. Контроллер. ObligationController обрабатывает входящие HTTP-запросы:

   • Валидация входных данных

   • Преобразование DTO → Model

   • Формирование HTTP-ответов

   • SSE стриминг событий


3. Сервис. Содержит всю бизнес-логику:

   • ObligationService — управление обязательствами (создание, обновление, Lazy Expiry)

   • PaymentService — обработка платежей и история оплат

   • SseService — отправка Server-Sent Events



4. Репозиторий. Обеспечивает доступ к данным:

   • ObligationRepository — JPA-интерфейс для таблицы obligations

   • PaymentRepository — JPA-интерфейс для таблицы payments



5. Нижний уровень — хранение в БД:

   • PostgreSQL 15

   • Таблицы: obligations, payments

   • Индексы для производительности

   • Триггеры для автоматического обновления



### Потоки данных
1. Создание обязательства
```text
Внешний мир → Контроллер → Сервис → Репозиторий → БД
                             ↓
                 Валидация и бизнес-правила
                             ↓
               Ответ (с предупреждением warning)
```

2. Получение списка (с Lazy Expiry)
```text
Внешний мир → Контроллер → Сервис → Репозиторий → БД
                             ↓
                     Проверка Lazy Expiry
                             ↓
                      Обновление статусов
                             ↓
                           Ответ
```

3. Оплата обязательства
```text
Внешний мир → Контроллер → Сервис → Репозиторий → БД
                             ↓
                 Проверка статуса - только ACTIVE
                             ↓
                    Создание записи платежа
                             ↓
            Сдвиг даты (рекуррентное) / Отмена (разовое)
                             ↓
                     SSE событие (broadcast)
                             ↓
                           Ответ
```



### Связи

| Связь | Тип | Описание |
|-------|-----|----------|
| `obligations` → `payments` | **One-to-Many** | Одно обязательство может иметь много платежей |
| `payments` → `obligations` | **Many-to-One** | Каждый платеж относится к одному обязательству |
| `ON DELETE CASCADE` | **Каскадное удаление** | При удалении обязательства удаляются все его платежи |


### Таблицы
Таблица obligations

| Поле | Тип | Обязательное | Описание |
|------|-----|-----|----------|
| `id` | `UUID` |  Да | Уникальный идентификатор (генерируется автоматически) |
| `title` | `VARCHAR(255)` |  Да | Название обязательства |
| `amount` | `DECIMAL(10,2)` |  Да | Сумма платежа |
| `currency` | `VARCHAR(3)` |  Да | Валюта (ISO 4217: RUB, USD, EUR) |
| `category` | `ENUM` |  Да | Категория: `SUBSCRIPTION`, `WARRANTY`, `BILL`, `INSURANCE` |
| `recurrence` | `ENUM` |  Нет | Периодичность: `MONTHLY`, `QUARTERLY`, `YEARLY` или `NULL` (разовое) |
| `next_payment_date` | `DATE` |  Да | Дата следующего платежа или истечения срока |
| `status` | `ENUM` |  Да | Статус: `ACTIVE`, `CANCELLED`, `EXPIRED` |
| `created_at` | `TIMESTAMP` |  Да | Дата создания (устанавливается автоматически) |
| `updated_at` | `TIMESTAMP` |  Да | Дата последнего обновления (обновляется автоматически) |


Таблица payments

| Поле | Тип | Обязательное | Описание |
|------|-----|----|----------|
| `id` | `UUID` | Да | Уникальный идентификатор (генерируется автоматически) |
| `obligation_id` | `UUID` | Да | Ссылка на обязательство (внешний ключ) |
| `amount` | `DECIMAL(10,2)` | Да | Фактически уплаченная сумма |
| `currency` | `VARCHAR(3)` | Да | Валюта платежа (RUB, USD, EUR) |
| `paid_at` | `TIMESTAMP` | Да | Момент фиксации оплаты |



### Индексы и триггеры
Индексы

| Индекс | Таблица | Колонки | Назначение |
|--------|---------|---------|------------|
| `idx_obligations_status` | obligations | `status` | Быстрый поиск по статусу |
| `idx_obligations_category` | obligations | `category` | Быстрый поиск по категории |
| `idx_obligations_next_payment_date` | obligations | `next_payment_date` | Быстрый поиск по дате |
| `idx_obligations_title_status` | obligations | `LOWER(title)`, `status` | Быстрый поиск по названию (регистронезависимый) |
| `idx_payments_obligation_id` | payments | `obligation_id` | Быстрый поиск платежей по обязательству |
| `idx_payments_paid_at` | payments | `paid_at` | Быстрый поиск по дате оплаты |

Триггеры

| Триггер | Таблица | Событие | Действие |
|---------|---------|---------|----------|
| `update_obligations_updated_at` | obligations | `BEFORE UPDATE` | Автоматически обновляет `updated_at` при изменении записи |

CHECK-констрейнты

```sql
-- Валидные валюты:
CHECK (currency IN ('RUB', 'USD', 'EUR'))

-- Валидные категории:
CHECK (category IN ('SUBSCRIPTION', 'WARRANTY', 'BILL', 'INSURANCE'))

-- Валидная периодичность (или NULL для разовых):
CHECK (recurrence IN ('MONTHLY', 'QUARTERLY', 'YEARLY') OR recurrence IS NULL)

-- Валидные статусы:
CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED'))
```
<br>

## Конфигурация

---

### Переменные окружения

| Переменная | По умолчанию | Описание |
|------------|--------------|----------|
| `SPRING_PROFILES_ACTIVE` | `default` | Активный профиль приложения |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/subscription_db` | URL подключения к базе данных |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Имя пользователя базы данных |
| `SPRING_DATASOURCE_PASSWORD` | `password` | Пароль базы данных |
| `SERVER_PORT` | `8080` | Порт приложения |
| `FLYWAY_ENABLED` | `true` | Включить/отключить Flyway миграции |
| `SPRING_JPA_DDL_AUTO` | `update` | Стратегия управления схемой БД |
| `SPRING_JPA_SHOW_SQL` | `true` | Выводить SQL-запросы в консоль |
| `LOG_LEVEL_APP` | `INFO` | Уровень логирования приложения |

### Профили

| Профиль | Файл конфигурации | Описание |
|---------|-------------------|----------|
| `default` | `application.yml` | Основная конфигурация (разработка) |
| `test` | `application-test.yml` | Профиль для тестирования (в `src/test/resources/`) |


<br>

## Установка

---

### Требования
```text
Java 21+ 
Docker & Docker Compose 
Maven 3.9+ 
PostgreSQL 15 (или через Docker)
```

### Клонирование

```bash
git clone https://github.com/yourusername/subscription-platform.git
cd subscription-platform
```

### Настройка окружения
```bash
# Скопируйте шаблон .env
cp .env.example .env


# Отредактируйте .env (измените пароль при необходимости)
POSTGRES_PASSWORD=password
SPRING_PROFILES_ACTIVE=dev
```
<br>

## Запуск

---

### Через Docker
```bash
# Первый запуск (сборка образа)
docker-compose up -d --build

# Последующие запуски
docker-compose up -d

# Проверка статуса
docker-compose ps

# Просмотр логов приложения
docker-compose logs -f app

# Просмотр логов БД
docker-compose logs -f postgres

# Остановка
docker-compose down -v
```

### Через Maven (локально)
```bash
# Запустить PostgreSQL через Docker
docker-compose up -d postgres

# Собрать и запустить
mvn clean package
java -jar target/subscription-platform-1.0.0.jar
```

### Проверка работоспособности
```bash
curl http://localhost:8080/api/obligations
# → []

Swagger UI: http://localhost:8080/api/swagger-ui.html
```

<br>


## API Документация

---


### Базовый URL
http://localhost:8080/api

### Swagger UI

http://localhost:8080/api/swagger-ui.html
<br>


## API Эндпоинты

---

### POST /obligations
#### Создание обязательства

Тело запроса:
```json
{
  "title": "СберПрайм",
  "amount": 299.00,
  "currency": "RUB",
  "category": "subscription",
  "recurrence": "monthly",
  "nextPaymentDate": "2024-02-15"
}
```

Ответ (успех):

```json
{
  "obligation": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "СберПрайм",
    "amount": 299.00,
    "currency": "RUB",
    "category": "SUBSCRIPTION",
    "recurrence": "MONTHLY",
    "nextPaymentDate": "2024-02-15",
    "status": "ACTIVE",
    "createdAt": "2024-01-15T10:00:00",
    "updatedAt": "2024-01-15T10:00:00"
  }
}
```

Ответ (с предупреждением):
```json
{
  "obligation": { ... },
  "warning": "Активное обязательство с таким названием уже существует"
}
```

Ответ (просроченное):

```json
{
  "obligation": {
    "status": "EXPIRED"
  }
}
```
<br>

### GET /obligations
#### Получение списка с фильтрацией
```text
Параметры:
 status - active, cancelled, expired
 category - subscription, warranty, bill, insurance
```
Пример:
```bash
GET /api/obligations?status=active&category=subscription
```
Ответ:

```json
[
  {
    "id": "...",
    "title": "Netflix",
    "amount": 9.99,
    "currency": "USD",
    "category": "SUBSCRIPTION",
    "recurrence": "MONTHLY",
    "nextPaymentDate": "2025-06-27",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```
<br>

### GET /obligations/upcoming?days=N
#### Предстоящие списания
```text
Параметры:
 days - количество дней вперед, по умолчанию - 7
```
Ответ:
```json
{
  "obligations": [
    {
      "id": "...",
      "title": "Netflix",
      "amount": 9.99,
      "currency": "USD",
      "category": "SUBSCRIPTION",
      "recurrence": "MONTHLY",
      "nextPaymentDate": "2025-06-27",
      "status": "ACTIVE"
    }
  ],
  "totals": {
    "RUB": 1490.00,
    "USD": 9.99
  },
  "renewalAlerts": [
    {
      "id": "...",
      "title": "Netflix",
      "nextPaymentDate": "2025-06-27",
      "amount": 9.99,
      "currency": "USD"
    }
  ]
}
```
<br>

### POST /obligations/{id}/pay
#### Оплата обязательства
```text
Результат:
 - рекуррентное: сдвиг даты на период
 - разовое: статус → CANCELLED
```
Ответ:
```json
{
  "obligation": {
    "id": "...",
    "nextPaymentDate": "2025-07-27",
    "status": "ACTIVE"
  },
  "payment": {
    "id": "...",
    "obligationId": "...",
    "amount": 9.99,
    "currency": "USD",
    "paidAt": "2025-06-27T10:00:00"
  }
}
```
Ошибки:
```text
 422 — Неактивное обязательство (status != ACTIVE)
 422 — Обязательство не найдено
```
<br>

### DELETE /obligations/{id}
#### Удаление обязательства (SSE событие)
Ответ: 
```text
 204 No Content
```
<br>

### GET /obligations/stream
#### SSE стриминг событий
```text
События:
 update — При изменении обязательства
 delete — При удалении обязательства
```
Пример клиента:
```javascript
const eventSource = new EventSource('/api/obligations/stream');
eventSource.onmessage = (event) => {
const data = JSON.parse(event.data);
console.log('SSE event:', data);
};
```
<br>


## Деплой

---

### Docker сборка
```bash
# Собрать образ
docker build -t subscription-platform:latest .

# Запустить с production конфигурацией
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://prod-host:5432/db \
  -e DB_USER=prod_user \
  -e DB_PASSWORD=prod_password \
  subscription-platform:latest
```
<br>

### Мониторинг
В приложении включен Spring Boot Actuator для сбора метрик и проверки состояния.

| Эндпоинт | Описание |
|----------|----------|
| `/actuator/health` | Проверка здоровья приложения и его зависимостей (БД) |
| `/actuator/info` | Информация о версии и сборке приложения |
| `/actuator/metrics` | Метрики производительности (JVM, HTTP-запросы, CPU) |
<br>


## Очистка

---

После завершения работы можно очистить систему от созданных артефактов

```bash
# 1. Остановить и удалить контейнеры и тома Docker
docker-compose down -v

# 2. Удалить образ приложения
docker rmi subscription-platform-app

# 3. Удалить образ postgres (если хотите полностью чистую БД)
docker rmi postgres:15

# 4. Проверить, что ничего не осталось
docker ps -a
docker images

# 5. Очистить собранные файлы Maven
mvn clean
```
<br>

## Лицензия

---

MIT License

Copyright (c) 2026 NikolayS2188

Permission is hereby granted...
