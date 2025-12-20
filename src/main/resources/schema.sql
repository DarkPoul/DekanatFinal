SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE users
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

-- Mail module tables
CREATE TABLE IF NOT EXISTS mail_chat
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    peer_email       VARCHAR(320) NOT NULL,
    display_name     VARCHAR(255),
    org_unit         VARCHAR(255),
    status           VARCHAR(32)  NOT NULL DEFAULT 'NEW',
    has_unprocessed  TINYINT(1)    NOT NULL DEFAULT 0,
    last_message_at  DATETIME,
    CONSTRAINT uq_mail_chat_peer_email UNIQUE (peer_email),
    INDEX idx_mail_chat_last_message_at (last_message_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS mail_message
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id      VARCHAR(512) NOT NULL,
    chat_id         BIGINT       NOT NULL,
    peer_email      VARCHAR(320) NOT NULL,
    folder          VARCHAR(64)  NOT NULL,
    uid             BIGINT       NOT NULL,
    sent_at         DATETIME,
    from_email      VARCHAR(320),
    to_email        VARCHAR(320),
    subject         VARCHAR(500),
    snippet         VARCHAR(1000),
    has_attachments TINYINT(1)   NOT NULL DEFAULT 0,
    direction       VARCHAR(8)   NOT NULL,
    CONSTRAINT uq_mail_message_message_id UNIQUE (message_id),
    INDEX idx_mail_message_peer_email_date (peer_email, sent_at),
    INDEX idx_mail_message_folder_uid (folder, uid),
    CONSTRAINT fk_mail_message_chat FOREIGN KEY (chat_id) REFERENCES mail_chat (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS mail_attachment_meta
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id   BIGINT       NOT NULL,
    part_id      VARCHAR(128) NOT NULL,
    filename     VARCHAR(500),
    content_type VARCHAR(255),
    size_bytes   BIGINT,
    CONSTRAINT fk_mail_attachment_message FOREIGN KEY (message_id) REFERENCES mail_message (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS mail_sync_state
(
    folder   VARCHAR(64) PRIMARY KEY,
    last_uid BIGINT
) ENGINE = InnoDB;
