# HeritageHub

HeritageHub is a Spring Boot + MySQL web platform for community-driven cultural heritage sharing, review, and moderation. It supports public browsing, registered user engagement, contributor submissions, and an administrator console for approval, taxonomy, archive, history, and complaint workflows.

## Tech Stack

- Backend: Java 17, Spring Boot 3.3.4, JdbcTemplate
- Frontend: HTML, CSS, JavaScript ES modules
- Database: MySQL 8.0+
- Build tool: Maven
- Security: BCrypt password hashing, server-side sessions, role-based access control
- Tests: JUnit 5, Mockito, H2 test schemas

## Features

- Authentication: registration, login, guest access, logout, remember-me cookie, account lock after repeated failed login attempts
- Roles: `USER`, `CONTRIBUTOR`, and `ADMIN`
- Profile: profile details, avatar upload, password update, email update, engagement summary
- Resource discovery: public resource list, filtering, sorting, detail page, view count, favorites
- Comments: post, reply, edit, soft delete, like, and report comments
- Contributor workflow: application submission, admin approval/rejection, rejected-application appeal thread
- Resource workflow: draft save, submission, file attachments, admin review, approval/rejection, revision, appeal thread
- Moderation: resource reports, comment reports, contributor appeals, admin complaint inbox, admin replies, reopen reported resources, delete reported comments
- Admin console: dashboard, contributor approval, resource review, taxonomy management, archive management, activity history, complaint management

## Project Structure

- `src/main/java/com/cpt202/auth`: controllers, services, repositories, DTOs, models, config, and exception handling
- `src/main/resources/static`: user-facing pages and JavaScript modules
- `src/main/resources/static/admin`: administrator console pages and JavaScript modules
- `src/main/resources/schema.sql`: MySQL schema created on application startup
- `src/main/resources/application.properties`: local runtime configuration with environment-variable overrides
- `postman/heritage-api.postman_collection.json`: API request collection for manual integration testing
- `sql/create-database.sql`: database creation helper
- `uploads/`: local uploaded files, ignored by Git

## MySQL Setup

The default application database is `heritage_platform`.

Create the database first:

```sql
CREATE DATABASE IF NOT EXISTS heritage_platform
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

You can also run the helper script:

```sql
SOURCE sql/create-database.sql;
```

Database credentials can be supplied with environment variables:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/heritage_platform?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
```

If these variables are not set, Spring Boot falls back to `src/main/resources/application.properties`.

## Run

Make sure Java 17 and Maven are installed, then run:

```bash
mvn spring-boot:run
```

If you are using the bundled Maven from this workspace on Windows:

```powershell
..\tools\apache-maven-3.9.14\bin\mvn.cmd spring-boot:run
```

Open:

- `http://localhost:8080/login.html`
- `http://localhost:8080/index.html`
- `http://localhost:8080/profile.html`
- `http://localhost:8080/submit-resource.html`
- `http://localhost:8080/admin/dashboard.html`
- `http://localhost:8080/admin/complaint-management.html`

## Demo Accounts

The application seeds these accounts automatically if they do not already exist:

- User: `demo@heritagehub.com` / `Password123!`
- Contributor: `contributor@heritagehub.com` / `Password123!`
- Admin: `Admin@qq.com` / `admin123456`

## Key API Areas

- Auth: `/api/auth/register`, `/api/auth/login`, `/api/auth/guest`, `/api/auth/me`, `/api/auth/logout`
- Profile: `/api/profile`, `/api/profile/avatar`, `/api/profile/password`, `/api/profile/email`
- Resources: `/api/resources`, `/api/resources/{resourceId}`, `/api/resources/submit`, `/api/resources/draft`
- Resource actions: `/api/resources/{resourceId}/favorite`, `/api/resources/{resourceId}/comments`, `/api/resources/{resourceId}/appeals`, `/api/resources/{resourceId}/reports`
- Comments: `/api/comments/{commentId}/like`, `/api/comments/{parentId}/reply`, `/api/comments/{commentId}`, `/api/comments/{commentId}/reports`
- Contributor applications: `/api/contributor-applications`, `/api/contributor-applications/current/appeals`
- Admin: `/api/admin/dashboard`, `/api/admin/resources/reviews`, `/api/admin/categories`, `/api/admin/tags`, `/api/admin/archives`, `/api/admin/history`
- Admin complaints: `/api/admin/complaints`, `/api/admin/complaints/{complaintType}/{complaintId}`, `/api/admin/complaints/{complaintType}/{complaintId}/reply`, `/api/admin/complaints/resource-report/{complaintId}/reopen`, `/api/admin/complaints/comment-report/{complaintId}/delete-comment`

## Postman

Import `postman/heritage-api.postman_collection.json` and set:

- `baseUrl`: `http://localhost:8080`

The collection includes login flows, resource and comment report submission, and the admin complaint inbox workflow.

## Tests

Run the full test suite:

```bash
mvn test
```

Run the complaint/reporting-related service tests:

```powershell
..\tools\apache-maven-3.9.14\bin\mvn.cmd -q "-Dtest=AdminConsoleServiceTest,ContributorApplicationServiceTest,ResourceServiceTest" test
```

## Repository Notes

- `target/`, `uploads/`, local SQL dumps, and generated ER diagram files are ignored by Git.
- Do not commit local database exports such as `sql/*_dump_*.sql` or `sql/*_full_*.sql`; they may contain user data and password hashes.
