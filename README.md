# Community Auth Demo

This is a Spring Boot + Maven login demo built with:

- Backend: Java 17, Spring Boot
- Build tool: Maven
- Frontend: HTML, CSS, JavaScript
- Database: MySQL

## Features

- User registration with email, username, and password validation
- Email and password login
- Guest session entry for read-only access
- Client-side empty-field validation
- Generic credential error response
- Warning after repeated failed attempts
- 15-minute account lock after too many failed logins
- Session-based login state with optional 30-day remember-me cookie
- Seeded demo accounts for user, contributor, and admin roles
- Role-aware session payload from `/api/auth/me`

## Project Structure

- `src/main/java`: backend controller, service, repository, config
- `src/main/resources/static`: authentication entry pages plus placeholder post-login pages
- `src/main/resources/schema.sql`: `users` table
- `sql/create-database.sql`: database creation script

## MySQL Setup

Run this first in MySQL:

```sql
CREATE DATABASE IF NOT EXISTS community_auth
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

Then update database credentials if needed:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/community_auth?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=123456
```

You can also override them with environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## Run

Make sure Java 17 and Maven are installed, then run:

```bash
mvn spring-boot:run
```

Visit:

- `http://localhost:8080/login.html`
- `http://localhost:8080/register.html`

## Demo Accounts

- User: `demo@heritagehub.com` / `Password123!`
- Contributor: `contributor@heritagehub.com` / `Password123!`
- Admin: `admin@heritagehub.com` / `AdminPass123!`

The application seeds these accounts automatically if they do not already exist.

## Integration Notes

- New registrations always create `USER` accounts.
- Contributor status is stored as a role in the database and can be updated later by other modules.
- Admin accounts are seeded directly and are not created through registration.
- Login responses return a `redirectUrl`; the downstream page at that URL can be implemented by other teammates.
- `/api/auth/me` now returns role and permission flags so later pages can decide what to show or disable.
