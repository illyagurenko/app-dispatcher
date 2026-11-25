# App Dispatcher Service

Высокопроизводительный сервис динамической маршрутизации банковских счетов и аутентификации вызывающих систем (Basic Auth + BCrypt) для распределенного конвейера процессинга (**app-changer**).

Реализован на **Kotlin 2.4.20** (Java 25), **Spring Boot 4.0.10**, **Spring Data JPA** и **Liquibase** с оптимизированным индексным поиском по базе данных **PostgreSQL 16** (поддержка батчей при 1 000 000+ счетов за < 1 мс).

---

## Архитектура и схема работы

Сервис выступает единой точкой маршрутизации (Routing Control Plane) и предоставляет целевые `jdbc_url` баз данных для пачек счетов:

```text
                       [ Сервис app-changer ]
                                  │
       POST /get-datasource-by-account (или /get-datasoure-by-account)
       Header: Authorization: Basic ...  (или Basic: ...)
                                  │
                                  ▼
             ┌─────────────────────────────────────────┐
             │          DispatcherController           │
             │    (Валидация DTO и заголовков @Valid)  │
             └─────────────────────────────────────────┘
                                  │
                                  ▼
             ┌─────────────────────────────────────────┐
             │           DispatcherService             │
             └─────────────────────────────────────────┘
                                  │
       1. Фиксация входящего запроса (STATUS: IN_PROGRESS)
                                  ▼
             ┌─────────────────────────────────────────┐
             │      Таблица APP_DISPATCHER_TRANS       │
             └─────────────────────────────────────────┘
                                  │
       2. Аутентификация и проверка прав (BCrypt + PERMISSION=1 + systemId)
                                  ▼
             ┌─────────────────────────────────────────┐
             │               AuthService               │
             │   (Таблица APP_DISPATCHER_PERMISSION)   │
             └─────────────────────────────────────────┘
                         /                 \
           [ Отказ: 401 / 403 ]       [ Доступ разрешен ]
                     │                         │
                     ▼                         ▼
        Статус ERROR в БД и      3. Батч-поиск активных счетов (STATUS = 'ACTIVE')
        ответ Error JSON            по B-Tree индексу (APP_DISPATCHER_DATASOURCE)
                                               │
                                               ▼
                                 4. Группировка счетов по jdbc_url
                                    (неразмеченные счета -> jdbc_url = "")
                                               │
                                               ▼
                                 5. Фиксация ответа (STATUS: SUCCESS, CODE: 0)
                                    в APP_DISPATCHER_TRANS и возврат JSON
```

---

## Стек технологий

* **Язык:** Kotlin 2.4.20 (JVM Toolchain 25)
* **Фреймворк:** Spring Boot 4.0.10 (Spring Web MVC, Spring Data JPA, Spring Validation)
* **Безопасность:** Spring Security Crypto (`BCryptPasswordEncoder`)
* **База данных:** PostgreSQL 16 (`postgres:16-alpine`)
* **Миграции БД:** Liquibase (SQL-формат)
* **Тестирование:** JUnit 5, Mockito Kotlin, Spring MockMvc, H2 (PostgreSQL mode)
* **Сборщик:** Gradle (Kotlin DSL)

---

## Порты и сетевая архитектура

Сервис взаимодействует с контейнерами в общей Docker-сети `food_bridge`:

| Компонент | Назначение | Порт хоста | Внутренний порт |
| :--- | :--- | :--- | :--- |
| **`app-dispatcher`** | REST API маршрутизации | `8085` | `8085` |
| **`postgres_dispatcher`** | База данных диспетчера (`dispatcher_db`) | `5434` | `5432` |

Конфигурация подключения задаётся через переменные окружения в `application.yaml`:
* `SERVER_PORT` (по умолчанию `8085`)
* `DB_HOST` (по умолчанию `localhost`)
* `DB_PORT` (по умолчанию `5434`)
* `DB_NAME` (по умолчанию `dispatcher_db`)
* `DB_USER` (по умолчанию `dispatcher_user`)
* `DB_PASSWORD` (по умолчанию `root`)

---

## База данных и Liquibase

Все таблицы располагаются в схеме **`ACC_APP_DISPATCHER`** и создаются автоматически при старте через Liquibase (`db.changelog-master.yaml`):

1. **`APP_DISPATCHER_PERMISSION`** — учетные записи систем-клиентов:
   * `LOGIN` — идентификатор системы (уникальный, например `APP_CHANGER`).
   * `PASS_BCRYPT` — пароль в формате BCrypt.
   * `PERMISSION` — флаг разрешения (`1` — доступ открыт, `0` — заблокирован).
   * По умолчанию миграция `002-insert-default-data.sql` создает запись для `APP_CHANGER` (пароль `12345678`).
2. **`APP_DISPATCHER_DATASOURCE`** — таблица роутинга счетов:
   * `ACCOUNT` — номер банковского счета (например `40817810000000000001`).
   * `JDBC_URL` — строка подключения к целевой БД (например `jdbc:postgresql://localhost:5432/changer_db`).
   * `JDBC_TYPE` — тип БД (`POSTGRES`).
   * `STATUS` — статус маршрута (`ACTIVE` — участвует в роутинге, `BLOCK` — игнорируется).
   * Составной индекс `idx_dispatcher_datasource_account_status` на `("ACCOUNT", "STATUS")` обеспечивает субмиллисекундный поиск пачек на миллионах записей.
3. **`APP_DISPATCHER_TRANS`** — аудит всех запросов и ответов (`ID` = `requestId`, `PROTOCOL_TYPE = 'HTTP'`, `REQ_DATA`, `RESP_DATA`, `STATUS`, `RESP_CODE`, `RESP_DESC`).

---

## REST API сервиса

Базовый URL: `http://localhost:8085`

### Получение источников данных по списку счетов

* **Эндпоинт:** `POST /get-datasource-by-account` (также поддерживается алиас `POST /get-datasoure-by-account`)
* **Авторизация:** заголовок `Authorization` или `Basic` в одном из форматов:
  * `Basic <base64(login:password)>` (например `Basic QVBQX0NIQU5HRVI6MTIzNDU2Nzg=`)
  * `Basic <login> <password>` (например `Basic APP_CHANGER 12345678`)
  * Значение `login` должно совпадать с полем `data.systemId`.

**Пример запроса:**
```json
{
  "data": {
    "systemId": "APP_CHANGER",
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "account": [
      "ACC_GPB_001",
      "ACC_GPB_002",
      "ACC_UNMAPPED_999"
    ]
  },
  "status": "Success",
  "error": null
}
```

**Пример успешного ответа (`200 OK`):**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "systemId": "APP_DISPATCHER",
  "status": "Success",
  "error": null,
  "data": {
    "datasource": [
      {
        "jdbc_url": "jdbc:postgresql://localhost:5432/changer_db",
        "account": [
          "ACC_GPB_001",
          "ACC_GPB_002"
        ]
      },
      {
        "jdbc_url": "",
        "account": [
          "ACC_UNMAPPED_999"
        ]
      }
    ]
  }
}
```

**Коды ошибок:**
* `400 Bad Request` (`VALIDATION_ERROR`) — невалидный формат UUID в `requestId`, пустой список `account` или некорректный JSON.
* `401 Unauthorized` (`ACCESS_DENIED`) — отсутствует заголовок авторизации.
* `403 Forbidden` (`ACCESS_DENIED`) — неверный логин/пароль, неактивное разрешение (`PERMISSION != 1`) или несовпадение `login` и `systemId`.

Готовые HTTP-запросы для запуска из IntelliJ IDEA находятся в файле `test.http`.

---

## Тестирование

В проекте разделены модульные тесты (`src/test`) и сквозные интеграционные тесты (`src/IntegrationTest` с использованием H2 в режиме PostgreSQL):

* **Запуск модульных (unit) тестов:**
  ```powershell
  .\gradlew.bat test
  ```
* **Запуск интеграционных тестов:**
  ```powershell
  .\gradlew.bat integrationTest
  ```
* **Полная проверка (unit + integration):**
  ```powershell
  .\gradlew.bat check integrationTest
  ```

---

## Нагрузочное тестирование (1 000 000 счетов)

Для подготовки БД к стресс-тесту на 1 миллион проводок используется скрипт `populate_million_accounts.sql`, который генерирует 1 000 000 счетов формата `40817810000000000001`..`40817810000001000000`:

```powershell
Get-Content populate_million_accounts.sql | docker exec -i postgres_dispatcher psql -U dispatcher_user -d dispatcher_db
```

---

## Пошаговая инструкция по запуску с нуля (Clone & Run Guide)

### Шаг 1. Проверка Docker-сети
Убедитесь, что общая сеть создана:
```bash
docker network create food_bridge
```

### Шаг 2. Запуск базы данных PostgreSQL
```bash
docker compose up -d
```
Дождитесь статуса `healthy` для контейнера `postgres_dispatcher` (порт `5434`).

### Шаг 3. Сборка и запуск приложения
* **Для Windows (PowerShell / CMD):**
  ```powershell
  .\gradlew.bat bootRun
  ```
* **Для Linux / macOS:**
  ```bash
  ./gradlew bootRun
  ```

### Шаг 4. Проверка работоспособности
Отправьте тестовый запрос через `curl`:
```powershell
curl.exe -X POST http://localhost:8085/get-datasoure-by-account `
  -H "Content-Type: application/json" `
  -H "Authorization: Basic QVBQX0NIQU5HRVI6MTIzNDU2Nzg=" `
  -d "{\"data\":{\"systemId\":\"APP_CHANGER\",\"requestId\":\"550e8400-e29b-41d4-a716-446655440000\",\"account\":[\"ACC_GPB_001\"]},\"status\":\"Success\"}"
```
Ожидаемый ответ: JSON со статусом `"Success"` и `jdbc_url: "jdbc:postgresql://localhost:5432/changer_db"`.
