CREATE TABLE IF NOT EXISTS tooth_images (
    id         BIGINT        AUTO_INCREMENT PRIMARY KEY,
    url        VARCHAR(500)  NOT NULL,
    created_at DATETIME(6)   DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)   DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6)   DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS tooth_analyses (
    id         BIGINT        AUTO_INCREMENT PRIMARY KEY,
    image_id   BIGINT        NOT NULL,
    status     VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    job_id      VARCHAR(255)  DEFAULT NULL,
    send_count  INT           NOT NULL DEFAULT 0,
    result       TEXT          DEFAULT NULL,
    last_sent_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at   DATETIME(6)   DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)   DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at   DATETIME(6)   DEFAULT NULL,
    FOREIGN KEY (image_id) REFERENCES tooth_images(id)
);

CREATE INDEX idx_tooth_analyses_status_last_sent_at
    ON tooth_analyses (status, last_sent_at);

CREATE INDEX idx_tooth_analyses_created_at
    ON tooth_analyses (created_at);

CREATE INDEX idx_tooth_analyses_status_created_at
    ON tooth_analyses (status, created_at);

CREATE TABLE shedlock (
                          name       VARCHAR(64)  NOT NULL PRIMARY KEY,
                          lock_until TIMESTAMP    NOT NULL,
                          locked_at  TIMESTAMP    NOT NULL,
                          locked_by  VARCHAR(255) NOT NULL
)
