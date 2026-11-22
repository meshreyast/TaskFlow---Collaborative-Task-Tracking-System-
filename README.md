# TaskFlow

A REST backend for task tracking and team collaboration: users create tasks, assign them to teammates,
and discuss them through comments and file attachments.

**Stack:** Java 17, Spring Boot 3.5, Spring Security (stateless JWT), Spring Data JPA / Hibernate,
MySQL, Maven. Tests run on an in-memory H2 database.

## Quick start

Requires JDK 17+, Maven 3.9+ and Docker (for MySQL).

```bash
docker compose up -d                              # MySQL on :5432
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run    # API on http://localhost:8080
```

- Swagger UI (try every endpoint, with a *Authorize* button for the JWT): http://localhost:8080/swagger-ui.html
- Tests: `mvn verify`

The `dev` profile only supplies a throwaway JWT secret. Without it the app refuses to start until
`JWT_SECRET` is set.

### Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | *(required, 32+ chars)* | HMAC signing key. Generate: `openssl rand -base64 48` |
| `JWT_EXPIRATION_MINUTES` | `60` | Token lifetime |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | local MySQL | Database connection |
| `DDL_AUTO` | `update` | Hibernate schema management (use `validate` with migrations in production) |
| `STORAGE_DIR` | `./uploads` | Where attachments are stored |

## Try it

```bash
BASE=http://localhost:8080/api

# register (returns a token) and create a personal task
TOKEN=$(curl -s $BASE/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Ada","email":"ada@example.com","password":"correct-horse-1"}' | jq -r .accessToken)

curl -s $BASE/tasks -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Write report","description":"Q4 numbers","dueDate":"2030-01-31"}'

# my open tasks, searching "report", soonest due first
curl -s "$BASE/tasks?assignedToMe=true&status=OPEN&q=report&sort=dueDate,asc" \
  -H "Authorization: Bearer $TOKEN"
```

## API

All endpoints except register/login need `Authorization: Bearer <token>`. Errors use
[RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) problem JSON; validation errors list the offending fields
under `errors`.

| Area | Endpoint | Description |
|---|---|---|
| Auth | `POST /api/auth/register` | Create an account (returns a token) |
| | `POST /api/auth/login` | Log in (returns a token) |
| | `POST /api/auth/logout` | Revoke the current token |
| Profile | `GET` / `PUT /api/users/me` | View / update name and email |
| | `PUT /api/users/me/password` | Change password |
| Teams | `POST /api/teams`, `GET /api/teams` | Create a team / list my teams |
| | `GET` / `PUT` / `DELETE /api/teams/{id}` | View / edit / delete (edit and delete: owner) |
| | `GET /api/teams/{id}/members` | List members |
| | `DELETE /api/teams/{id}/members/{userId}` | Owner removes a member, or a member leaves |
| | `POST` / `GET /api/teams/{id}/invitations` | Owner invites by email / lists invitations |
| | `GET /api/invitations` | My pending invitations |
| | `POST /api/invitations/{id}/accept` or `/decline` | Answer an invitation |
| Tasks | `POST /api/tasks` | Create (add `teamId` for a team task, `assigneeId` to assign) |
| | `GET /api/tasks` | List, filter, search, sort, paginate (below) |
| | `GET` / `PUT` / `DELETE /api/tasks/{id}` | View / edit / delete |
| | `PATCH /api/tasks/{id}/status` | `OPEN`, `IN_PROGRESS` or `COMPLETED` |
| | `PUT /api/tasks/{id}/assignee` | Assign to a team member (`null` unassigns) |
| Comments | `POST` / `GET /api/tasks/{id}/comments` | Add / list |
| | `PUT` / `DELETE /api/tasks/{id}/comments/{commentId}` | Edit (author) / delete (author or team owner) |
| Attachments | `POST /api/tasks/{id}/attachments` | Upload (`multipart/form-data`, part `file`, max 10 MB) |
| | `GET /api/tasks/{id}/attachments` | List |
| | `GET /api/tasks/{id}/attachments/{attId}/download` | Download |
| | `DELETE /api/tasks/{id}/attachments/{attId}` | Delete (uploader, task creator or team owner) |

**`GET /api/tasks` query parameters:** `status`, `priority`, `teamId`, `assigneeId`, `assignedToMe`,
`overdue`, `dueFrom` / `dueTo` (inclusive, `yyyy-MM-dd`), `q` (case-insensitive search in title and
description), `page`, `size` (max 100), `sort` (`createdAt`, `updatedAt`, `dueDate` or `title`, e.g.
`sort=dueDate,asc`).

## Design notes

- **Teams and tasks.** A task without a `teamId` is personal: only its creator can see it. A team task is
  visible to every member of that team, and can only be assigned to a member.
- **Permissions.** Members can view, comment and attach. The task creator, its assignee or the team owner
  can edit, change status and assign. The creator or team owner can delete. Resources a user cannot see
  return `404`, so their existence is not leaked.
- **Invitations.** The owner invites by email, and the invitee accepts or declines. The invitee can register
  after being invited, since invitations are matched by email.
- **Auth.** Passwords are hashed with BCrypt. JWTs are signed (HS256+) and carry a unique id, and logging
  out stores that id in a revocation table, so a logged-out token stops working immediately. Expired
  revocations are purged hourly.
- **Attachments** are stored on local disk under generated keys (file names never touch the path) and are
  always served as downloads. Implement `FileStorageService` against S3 or similar to run several instances.

## Known limitations

- The schema is generated by Hibernate. Add Flyway or Liquibase migrations before production.
- No rate limiting or account lockout on login, no email verification and no password reset.
- Uploads are not virus-scanned or restricted by file type.
