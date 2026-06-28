CREATE TABLE user_profile (
    id              BIGSERIAL PRIMARY KEY,
    telegram_id     BIGINT UNIQUE NOT NULL,
    gender          VARCHAR(10)   NOT NULL,
    age             INTEGER       NOT NULL,
    height_cm       INTEGER       NOT NULL,
    weight_kg       DECIMAL(5, 2) NOT NULL,
    activity_level  VARCHAR(20)   NOT NULL,
    daily_target_kcal INTEGER     NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE food_log (
    id            BIGSERIAL PRIMARY KEY,
    telegram_id   BIGINT        NOT NULL,
    logged_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    dish          VARCHAR(255)  NOT NULL,
    grams         INTEGER       NOT NULL,
    calories      INTEGER       NOT NULL,
    protein_g     DECIMAL(6, 2) NOT NULL,
    fat_g         DECIMAL(6, 2) NOT NULL,
    carbs_g       DECIMAL(6, 2) NOT NULL,
    confidence    VARCHAR(20),
    portion_note  TEXT,
    photo_file_id VARCHAR(255)
);

CREATE TABLE weight_log (
    id          BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT        NOT NULL,
    logged_date DATE          NOT NULL DEFAULT CURRENT_DATE,
    weight_kg   DECIMAL(5, 2) NOT NULL,
    UNIQUE (telegram_id, logged_date)
);

CREATE INDEX idx_food_log_user_date ON food_log (telegram_id, logged_at);
CREATE INDEX idx_weight_log_user_date ON weight_log (telegram_id, logged_date);
