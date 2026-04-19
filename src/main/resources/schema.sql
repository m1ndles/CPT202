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
    period      VARCHAR(100)                                     COMMENT 'historical period, e.g. Ming Dynasty',
    place       VARCHAR(60)  NOT NULL                            COMMENT 'city or region where the heritage is located',
    description TEXT                                             COMMENT 'full introduction paragraph',
    thumbnail   VARCHAR(500)                                     COMMENT 'cover image URL for list cards',
    copyright   VARCHAR(500)                                     COMMENT 'licence or attribution notice',
    tracking_id VARCHAR(40) UNIQUE                               COMMENT 'submission tracking identifier for review workflow',
    status      VARCHAR(20)  NOT NULL DEFAULT 'APPROVED'         COMMENT 'workflow state: APPROVED / PENDING / REJECTED',
    view_count  INT          NOT NULL DEFAULT 0                  COMMENT 'incremented each time the detail page is visited',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'row creation timestamp',
    owner_user_id BIGINT     NULL                                COMMENT 'contributor account that created the resource draft/submission',
    owner_username VARCHAR(60) NULL                              COMMENT 'display name snapshot of the owning contributor',
    FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE SET NULL
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

CREATE TABLE IF NOT EXISTS resource_favorites (
    resource_id BIGINT       NOT NULL                            COMMENT 'favorited resource id',
    user_id     BIGINT       NOT NULL                            COMMENT 'registered user who saved the resource',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'when the resource was favorited',
    PRIMARY KEY (resource_id, user_id),
    FOREIGN KEY (resource_id) REFERENCES heritage_resources(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT = 'saved resource list for registered users';

CREATE TABLE IF NOT EXISTS resource_appeal_messages (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique appeal message identifier',
    resource_id BIGINT       NOT NULL                            COMMENT 'resource currently under revision',
    sender_role VARCHAR(20)  NOT NULL                            COMMENT 'CONTRIBUTOR / ADMIN / SYSTEM',
    sender_name VARCHAR(120) NOT NULL                            COMMENT 'display name shown in the appeal thread',
    content     TEXT         NOT NULL                            COMMENT 'appeal or clarification message body',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'when the message was created',
    FOREIGN KEY (resource_id) REFERENCES heritage_resources(id) ON DELETE CASCADE
) COMMENT = 'appeal and clarification thread for rejected resources';

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

CREATE TABLE IF NOT EXISTS comment_report_threads (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique comment report thread identifier',
    comment_id       BIGINT       NOT NULL                            COMMENT 'reported comment id',
    reporter_user_id BIGINT       NOT NULL                            COMMENT 'registered reporter account id',
    reporter_name    VARCHAR(120) NOT NULL                            COMMENT 'display name of the reporter',
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN'            COMMENT 'OPEN / REPLIED / RESOLVED',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'thread creation time',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'thread last update time',
    UNIQUE KEY uk_comment_report_thread (comment_id, reporter_user_id),
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    FOREIGN KEY (reporter_user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT = 'per-user report thread for a comment';

CREATE TABLE IF NOT EXISTS comment_report_messages (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique comment report message identifier',
    thread_id   BIGINT       NOT NULL                            COMMENT 'linked comment report thread id',
    sender_role VARCHAR(20)  NOT NULL                            COMMENT 'USER / ADMIN',
    sender_name VARCHAR(120) NOT NULL                            COMMENT 'display name of the sender',
    content     TEXT         NOT NULL                            COMMENT 'report message content',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'message creation time',
    FOREIGN KEY (thread_id) REFERENCES comment_report_threads(id) ON DELETE CASCADE
) COMMENT = 'conversation messages inside a comment report thread';

CREATE TABLE IF NOT EXISTS contributor_applications (
    id                   BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique application identifier',
    user_id              BIGINT       NOT NULL                            COMMENT 'applicant account id',
    full_name            VARCHAR(120) NOT NULL                            COMMENT 'displayed applicant full name',
    expertise_field      VARCHAR(120) NOT NULL                            COMMENT 'main domain of contribution',
    motivation_statement TEXT         NOT NULL                            COMMENT 'why the user wants contributor access',
    portfolio_link       VARCHAR(500) NULL                                COMMENT 'external portfolio or uploaded file URL',
    attachment_name      VARCHAR(255) NULL                                COMMENT 'uploaded supporting file original name',
    attachment_url       VARCHAR(500) NULL                                COMMENT 'uploaded supporting file access URL',
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING'          COMMENT 'PENDING / APPROVED / REJECTED',
    rejection_comments   TEXT         NULL                                COMMENT 'review feedback when rejected',
    submitted_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'application submission time',
    reviewed_at          DATETIME     NULL                                COMMENT 'when the application was reviewed',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT = 'registered user requests to become contributors';

CREATE TABLE IF NOT EXISTS contributor_application_appeal_messages (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique appeal message identifier',
    application_id BIGINT       NOT NULL                            COMMENT 'linked contributor application id',
    sender_role    VARCHAR(20)  NOT NULL                            COMMENT 'APPLICANT / ADMIN',
    sender_name    VARCHAR(120) NOT NULL                            COMMENT 'display name of the sender',
    content        TEXT         NOT NULL                            COMMENT 'appeal or reply content',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'message creation time',
    FOREIGN KEY (application_id) REFERENCES contributor_applications(id) ON DELETE CASCADE
) COMMENT = 'conversation thread for rejected contributor applications';

CREATE TABLE IF NOT EXISTS resource_report_threads (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique report thread identifier',
    resource_id      BIGINT       NOT NULL                            COMMENT 'reported resource id',
    reporter_user_id BIGINT       NOT NULL                            COMMENT 'registered reporter account id',
    reporter_name    VARCHAR(120) NOT NULL                            COMMENT 'display name of the reporter',
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN'            COMMENT 'OPEN / REPLIED / IN_REVIEW / RESOLVED',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'thread creation time',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'thread last update time',
    UNIQUE KEY uk_resource_report_thread (resource_id, reporter_user_id),
    FOREIGN KEY (resource_id) REFERENCES heritage_resources(id) ON DELETE CASCADE,
    FOREIGN KEY (reporter_user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT = 'per-user report thread for a resource';

CREATE TABLE IF NOT EXISTS resource_report_messages (
    id         BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique report message identifier',
    thread_id  BIGINT       NOT NULL                            COMMENT 'linked resource report thread id',
    sender_role VARCHAR(20) NOT NULL                            COMMENT 'USER / ADMIN',
    sender_name VARCHAR(120) NOT NULL                           COMMENT 'display name of the sender',
    content    TEXT         NOT NULL                            COMMENT 'report or reply message content',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'message creation time',
    FOREIGN KEY (thread_id) REFERENCES resource_report_threads(id) ON DELETE CASCADE
) COMMENT = 'conversation messages inside a resource report thread';

CREATE TABLE IF NOT EXISTS admin_categories (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique category identifier',
    name        VARCHAR(80)  NOT NULL UNIQUE                    COMMENT 'category display name',
    description TEXT         NOT NULL                           COMMENT 'category description shown in admin console',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'          COMMENT 'ACTIVE / INACTIVE',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'last update time'
) COMMENT = 'admin managed taxonomy categories';

CREATE TABLE IF NOT EXISTS admin_tags (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique tag identifier',
    name        VARCHAR(80)  NOT NULL UNIQUE                    COMMENT 'tag display name',
    description TEXT         NOT NULL                           COMMENT 'tag description shown in admin console',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'          COMMENT 'ACTIVE / INACTIVE',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'last update time'
) COMMENT = 'admin managed taxonomy tags';

CREATE TABLE IF NOT EXISTS admin_archive_records (
    id                  BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique archive record identifier',
    resource_id         BIGINT       NOT NULL UNIQUE                     COMMENT 'resource hidden from public visibility',
    contributor_label   VARCHAR(160) NULL                                COMMENT 'displayed contributor label in the archive table',
    archived_by         VARCHAR(120) NOT NULL                            COMMENT 'admin username or system marker',
    archive_reason      TEXT         NULL                                COMMENT 'why the resource was archived',
    publication_history TEXT         NULL                                COMMENT 'summary of the publication lifecycle',
    original_metadata   TEXT         NULL                                COMMENT 'resource metadata snapshot for archive detail',
    archived_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT 'archive creation time',
    FOREIGN KEY (resource_id) REFERENCES heritage_resources(id) ON DELETE CASCADE
) COMMENT = 'archived heritage resources hidden from public visibility';

CREATE TABLE IF NOT EXISTS admin_activity_history (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT 'unique activity identifier',
    action_type   VARCHAR(80)  NOT NULL                           COMMENT 'short action label used for filtering',
    target_type   VARCHAR(80)  NOT NULL                           COMMENT 'resource, contributor, category, tag, archive',
    target_name   VARCHAR(200) NOT NULL                           COMMENT 'displayed target name',
    operator_name VARCHAR(120) NOT NULL                           COMMENT 'who performed the action',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'activity time',
    details       TEXT         NULL                               COMMENT 'human readable activity details'
) COMMENT = 'admin activity audit trail';
