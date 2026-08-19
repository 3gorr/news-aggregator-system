# job board

Консольный агрегатор вакансий на Java 21. Тянет данные из HeadHunter API и career.habr.com, сохраняет в SQLite, даёт CLI для просмотра, поиска, фильтрации, экспорта (CSV/JSON/HTML), сбора статистики и фоновой автообновляемости.

## Технологии

| Слой | Что используется |
|---|---|
| Язык | Java 21 |
| Сборка | Maven |
| CLI | Picocli 4.7 |
| БД | SQLite (xerial sqlite-jdbc) + HikariCP |
| Миграции | Flyway 9 |
| HTTP | `java.net.http.HttpClient` (JDK) |
| HTML-парсинг | Jsoup |
| JSON | Jackson |
| Логи | SLF4J + Logback |
| Тесты | JUnit 5 + Mockito + AssertJ |

## Сборка и запуск

Требуется JDK 21+. Maven берёт SQLite-нативку под нужную платформу автоматически.

```bash
# собрать fat-jar
mvn package

# запустить
java -jar target/joboard.jar --help

# прогнать тесты
mvn test
```

База данных создаётся в текущей директории как файл `joboard.db` при первом запуске. Миграции применяются автоматически (Flyway).

## Быстрый старт

```bash
# посмотреть список настроенных источников
java -jar target/joboard.jar sources

# подтянуть вакансии (1 страница с каждого источника)
java -jar target/joboard.jar fetch --query="java developer"

# вывести 20 свежих вакансий
java -jar target/joboard.jar list

# полнотекстовый поиск
java -jar target/joboard.jar search "kotlin" --min-salary=200000

# детали по ID
java -jar target/joboard.jar show 42

# экспортировать в HTML
java -jar target/joboard.jar export --format=html --out=jobs.html

# статистика
java -jar target/joboard.jar stats --by=city
java -jar target/joboard.jar stats --salary --query=java

# подписаться на уведомления
java -jar target/joboard.jar notify add --name=java-msk --query=java --city=Москва --min-salary=200000

# фоновое обновление каждые 30 минут
java -jar target/joboard.jar daemon --interval=30m
```

## Полный список команд

```
fetch    [--source=hh|habr_career] [-q TEXT] [--pages N] [--per-page N]
list     [--city] [--company] [--source] [--min-salary] [--max-salary]
         [--sort=DATE_DESC|DATE_ASC|SALARY_DESC|SALARY_ASC|COMPANY_ASC]
         [-n LIMIT]
search   <query> [фильтры как у list]
show     <id>
export   --format=csv|json|html [-o FILE] [фильтры]
stats    --by=CITY|COMPANY|SOURCE [-n LIMIT]
stats    --salary [--query=TITLE]
notify   add --name=NAME [--query] [--city] [--min-salary]
notify   list
notify   remove NAME
history  [-n LIMIT]
daemon   --interval=30s|5m|1h [--source] [-q] [--pages] [--per-page]
sources  [enable|disable CODE]
```

У каждой команды есть `--help`.

## Архитектура

```
com.github._3gorr.joboard
├── Main / AppContext             — точка входа и DI-сборка
├── cli/                          — Picocli команды + JoboardCommandFactory (IFactory)
├── model/                        — records: Vacancy, Salary, Source, SearchFilter, ...
├── source/                       — VacancySource + HhApiSource, HabrCareerSource, HttpFetcher
├── repository/                   — *Repository интерфейсы + Jdbc-реализации
├── service/                      — FetchService, SearchService, StatsService,
│                                   NotificationService, SchedulerService
├── export/                       — Exporter + Csv/Json/Html + ExporterFactory
└── db/                           — DataSourceFactory, AppConfig, миграции (resources)
```

### SOLID

- **SRP** — каждый класс имеет один ответственность: `VacancyPrinter` форматирует, `FetchService` оркестрирует, `JdbcVacancyRepository` только SQL. `AppConfig` отделён от `DataSourceFactory`.
- **OCP** — добавить новый источник = новый класс, реализующий `VacancySource`, и одна строчка в `AppContext`. Аналогично с экспортёрами (`Exporter`).
- **LSP** — все реализации `VacancySource`, `Exporter`, `VacancyRepository` полностью взаимозаменяемы; в тестах используется `FakeSource` вместо HhApiSource.
- **ISP** — отдельные интерфейсы для каждой роли: `VacancyRepository`, `SourceRepository`, `StatsRepository`, `NotificationRepository`, `VacancyHistoryRepository` (а не один большой).
- **DIP** — `FetchService`, `SearchService` и т. д. зависят только от интерфейсов; конкретные классы собираются в `AppContext`. Источники зависят от `HttpFetcher` (интерфейс), что делает их полностью юнит-тестируемыми без сети.

### GRASP

- **Information Expert** — `NotificationFilter.matches(Vacancy)` живёт на самой `NotificationFilter`, т.к. она «знает», как сравнивать критерии.
- **Creator** — `AppContext` создаёт все сервисы; `JoboardCommandFactory` создаёт команды (Picocli `IFactory`).
- **Controller** — CLI-команды (`FetchCommand`, `ListCommand` и т. п.) — тонкие контроллеры, не содержат бизнес-логики.
- **Low Coupling** — между слоями только интерфейсы; `FetchService` ничего не знает про SQLite, Picocli или конкретные HTTP-библиотеки.
- **High Cohesion** — пакеты сгруппированы по слоям (cli / service / repository / source / export).
- **Polymorphism** — `Exporter` диспатч через `ExporterFactory.get(format)` вместо if/else по строке-формату.
- **Pure Fabrication** — `ContentHasher`, `DurationParser` — утилитарные классы, не отображающие реальные сущности, но решающие технические задачи.
- **Observer** — `FetchListener` (`NotificationService` слушает `FetchService`).

### Ключевые решения

- **SQLite + Flyway** вместо Postgres/Docker — чтобы собирать и запускать в один `java -jar` без внешних зависимостей. Миграции версионируются (`V1__init.sql`, `V2__seed_sources.sql`).
- **Даты как ISO-8601 TEXT** в БД — сортируются лексикографически, парсятся `Instant.parse()`. SQLite не имеет настоящего TIMESTAMP, а INTEGER (unix seconds) теряет читаемость.
- **content_hash (SHA-256)** для детекции изменений вакансии: вместо field-by-field сравнения хешируем значимые поля; при совпадении хеша запись не трогаем.
- **Дедупликация** по `UNIQUE(source_id, external_id)` на уровне БД и проверкой на стороне `FetchService`.
- **Case-sensitive фильтрация по `city` / `company`** — SQLite без ICU-extension не умеет lowercase для кириллицы. Данные из источников приходят канонически, поэтому это рабочий компромисс.
- **`Clock` инжектится** в `FetchService` и `JdbcVacancyHistoryRepository` — детерминированные тесты.
- **`HttpFetcher` интерфейс** отделяет источники от реальной сети — все source-тесты гоняются на сохранённых JSON/HTML фикстурах.
- **`SchedulerService` отдельно от `DaemonCommand`** — сервис ничего не знает про CLI, его легко переиспользовать (или подменить в тестах).

## Тесты

```bash
mvn test
```

Покрытие:
- Репозитории — интеграционные тесты на временной SQLite-БД (с реальными Flyway-миграциями).
- Источники — на сохранённых фикстурах через `StubHttpFetcher` (без сети).
- Сервисы — комбинация Mockito (на интерфейсах) и `TestDatabase`.
- CLI — `VacancyPrinter` и `DurationParser` юнит-тестами.
- Планировщик — реальный `ScheduledExecutorService` с короткими интервалами.

Текущий счёт: **82 теста, все зелёные**.

## Источники: особенности

**Хабр Карьера** — HTML-парсинг через Jsoup. Работает «из коробки» без авторизации. Учитывает обе раскладки карточек: с чипами (`.basic-chip` + `<use xlink:href="...placemark">` для города) и старую без чипов. Если у вакансии не указана зарплата, Хабр показывает оценочную («Похожие специалисты получают…») — мы её **игнорируем** и сохраняем `salary not specified`.

**HeadHunter API** (`api.hh.ru/vacancies`) — JSON, без авторизации в спецификации, но **HH блокирует** запросы по IP / User-Agent при подозрении на бота. С некоторых сетей (хостинги, прокси, ряд дата-центров) endpoint `/vacancies` стабильно возвращает `403 forbidden`, при этом `/areas` отвечает `200`. Парсер технически корректен (проверен фикстурами в `HhApiSourceTest`) — если на твоей сети HH 403'ит, попробуй с домашнего/рабочего интернета. На крайний случай можно зарегистрировать приложение в `https://dev.hh.ru/admin` и подставить OAuth-токен (расширение `JdkHttpFetcher` под добавление `Authorization` header — однострочная правка).

## Структура БД

```sql
source              (id, code UNIQUE, name, base_url, enabled)
vacancy             (id, source_id, external_id, url, title, company, city,
                     salary_from, salary_to, salary_currency, employment_type,
                     description, requirements,
                     published_at, fetched_at, content_hash,
                     UNIQUE(source_id, external_id))
vacancy_history     (id, vacancy_id, source_id, external_id, operation, occurred_at)
notification_filter (id, name UNIQUE, query, city, min_salary, created_at)
```

## Лицензия / автор

Учебный проект, автор: [3gorr](https://github.com/3gorr).
