CREATE TABLE IF NOT EXISTS users (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique user identifier',
    email           VARCHAR(120) NOT NULL UNIQUE                     COMMENT 'login email, used as account credential',
    username        VARCHAR(60)  NOT NULL UNIQUE                     COMMENT 'display name, must be unique',
    password_hash   VARCHAR(100) NOT NULL                            COMMENT 'bcrypt-encoded password',
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER'             COMMENT 'USER / CONTRIBUTOR / ADMIN',
    failed_attempts INT          NOT NULL DEFAULT 0                  COMMENT 'consecutive failed login count',
    locked_until    DATETIME     NULL                                COMMENT 'account locked until this time after too many failures',
    last_login_at   DATETIME     NULL                                COMMENT 'timestamp of last successful login',
    avatar_url      VARCHAR(500) NULL                                COMMENT 'profile avatar image URL',
    bio             TEXT         NULL                                COMMENT 'short personal biography shown on the profile page',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'account creation time'
) COMMENT = 'platform accounts for authentication and authorisation';

CREATE TABLE IF NOT EXISTS heritage_resources (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique resource identifier',
    title       VARCHAR(200) NOT NULL                            COMMENT 'display name shown on cards and detail page',
    title_en    VARCHAR(200)                                     COMMENT 'English title for bilingual reference',
    category    VARCHAR(60)  NOT NULL                            COMMENT 'classification, e.g. Classical Garden, Traditional Craft',
    place       VARCHAR(60)  NOT NULL                            COMMENT 'city or region where the heritage is located',
    description TEXT                                             COMMENT 'full introduction paragraph',
    thumbnail   VARCHAR(500)                                     COMMENT 'cover image URL for list cards',
    copyright   VARCHAR(500)                                     COMMENT 'licence or attribution notice',
    status      VARCHAR(20)  NOT NULL DEFAULT 'APPROVED'         COMMENT 'workflow state: APPROVED / PENDING / REJECTED',
    view_count  INT          NOT NULL DEFAULT 0                  COMMENT 'incremented each time the detail page is visited',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'row creation timestamp'
) COMMENT = 'cultural heritage entries published on the platform';

CREATE TABLE IF NOT EXISTS heritage_resource_tags (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT 'surrogate key',
    resource_id BIGINT      NOT NULL                   COMMENT 'owning resource',
    tag         VARCHAR(60) NOT NULL                   COMMENT 'tag label, e.g. UNESCO World Heritage',
    FOREIGN KEY (resource_id) REFERENCES heritage_resources(id) ON DELETE CASCADE
) COMMENT = 'one row per tag attached to a heritage resource';

CREATE TABLE IF NOT EXISTS heritage_resource_files (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT 'surrogate key',
    resource_id BIGINT       NOT NULL                   COMMENT 'owning resource',
    name        VARCHAR(200) NOT NULL                   COMMENT 'file display name',
    type        VARCHAR(20)  NOT NULL                   COMMENT 'media type: image / document',
    url         VARCHAR(500) NOT NULL                   COMMENT 'download or preview URL',
    FOREIGN KEY (resource_id) REFERENCES heritage_resources(id) ON DELETE CASCADE
) COMMENT = 'images or documents associated with a heritage resource';

CREATE TABLE IF NOT EXISTS heritage_resource_links (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT 'surrogate key',
    resource_id BIGINT       NOT NULL                   COMMENT 'owning resource',
    label       VARCHAR(200) NOT NULL                   COMMENT 'anchor text shown to the user',
    url         VARCHAR(500) NOT NULL                   COMMENT 'target URL',
    FOREIGN KEY (resource_id) REFERENCES heritage_resources(id) ON DELETE CASCADE
) COMMENT = 'external reference URLs for a heritage resource';

CREATE TABLE IF NOT EXISTS comments (
    id          BIGINT   PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique comment identifier',
    resource_id BIGINT   NOT NULL                            COMMENT 'the resource being commented on',
    user_id     BIGINT   NOT NULL                            COMMENT 'author, must be a logged-in user, not guest',
    content     TEXT     NOT NULL                            COMMENT 'comment body, max 500 chars enforced by app',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'when the comment was posted',
    FOREIGN KEY (resource_id) REFERENCES heritage_resources(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT = 'user feedback on approved heritage resources';

CREATE TABLE IF NOT EXISTS comment_likes (
    comment_id BIGINT   NOT NULL                            COMMENT 'the comment being liked',
    user_id    BIGINT   NOT NULL                            COMMENT 'the user who liked it',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'when the like was recorded',
    PRIMARY KEY (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT = 'one like per user per comment, composite PK prevents duplicates';
