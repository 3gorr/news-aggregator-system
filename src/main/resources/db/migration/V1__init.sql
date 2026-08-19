CREATE TABLE source (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    code      TEXT    NOT NULL UNIQUE,
    name      TEXT    NOT NULL,
    base_url  TEXT    NOT NULL,
    enabled   INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE vacancy (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    source_id        INTEGER NOT NULL REFERENCES source (id),
    external_id      TEXT    NOT NULL,
    url              TEXT    NOT NULL,
    title            TEXT    NOT NULL,
    company          TEXT,
    city             TEXT,
    salary_from      INTEGER,
    salary_to        INTEGER,
    salary_currency  TEXT,
    employment_type  TEXT,
    description      TEXT,
    requirements     TEXT,
    published_at     TEXT    NOT NULL,
    fetched_at       TEXT    NOT NULL,
    content_hash     TEXT    NOT NULL,
    UNIQUE (source_id, external_id)
);

CREATE INDEX idx_vacancy_city          ON vacancy (city);
CREATE INDEX idx_vacancy_published_at  ON vacancy (published_at);
CREATE INDEX idx_vacancy_salary_from   ON vacancy (salary_from);
CREATE INDEX idx_vacancy_source        ON vacancy (source_id);

CREATE TABLE vacancy_history (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    vacancy_id   INTEGER,
    source_id    INTEGER NOT NULL,
    external_id  TEXT    NOT NULL,
    operation    TEXT    NOT NULL,
    occurred_at  TEXT    NOT NULL
);

CREATE INDEX idx_history_occurred_at ON vacancy_history (occurred_at);

CREATE TABLE notification_filter (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,
    query       TEXT,
    city        TEXT,
    min_salary  INTEGER,
    created_at  TEXT    NOT NULL
);
