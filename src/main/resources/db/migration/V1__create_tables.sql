-- Сырые события от Serving. event_id — первичный ключ: повторно присланное событие не сохраняется второй раз.
CREATE TABLE events (
    event_id    VARCHAR(255) PRIMARY KEY,
    account_id  VARCHAR(255) NOT NULL,
    event_time  TIMESTAMPTZ  NOT NULL,
    shown       BOOLEAN      NOT NULL,
    received_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX events_event_time_idx ON events (event_time);

-- Почасовые агрегаты: одна строка на аккаунт и час.
CREATE TABLE hourly_stats (
    account_id    VARCHAR(255) NOT NULL,
    hour          TIMESTAMPTZ  NOT NULL,
    requests      BIGINT       NOT NULL,
    shown         BIGINT       NOT NULL,
    aggregated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (account_id, hour)
);
