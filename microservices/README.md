# Gym Management System Backend

This folder contains the five existing Spring Boot services. The gateway is the frontend entry point. Data-owning services use separate PostgreSQL databases.

## Architecture and ports

| Service | Port | Responsibility |
| --- | ---: | --- |
| API Gateway | 8083 | Routes requests and checks JWT roles |
| Member Service | 8081 | Member profiles, admin CRUD, self profile |
| Membership Service | 8082 | Plans, enrollment, renewal, cancellation, expiry, simulated payments |
| Trainer Service | 8084 | Trainer browsing, search, admin CRUD |
| Auth Service | 8085 | Registration, login, JWT issuance, first-admin setup |

Routes preserve the `/api` prefix: `/api/auth/**` -> Auth; `/api/members/**` -> Member; `/api/memberships/**` -> Membership; `/api/trainers/**` -> Trainer.

## Software and database setup

Use JDK 25 (the POMs set java.version=25), the included Maven wrappers, and PostgreSQL. Create four databases: gym_auth_db, gym_member_db, gym_membership_db, gym_trainer_db. For example, in psql:

```sql
CREATE DATABASE gym_auth_db;
CREATE DATABASE gym_member_db;
CREATE DATABASE gym_membership_db;
CREATE DATABASE gym_trainer_db;
```

There are no Flyway/Liquibase migrations. Hibernate ddl-auto=update creates/updates tables on startup. Back up data before changing entity mappings. For production, add reviewed versioned migrations and change Hibernate to validate.

## Environment and secrets

Copy .env.example to a private .env and replace placeholders. Never commit real secrets. Spring Boot does not load dotenv automatically: set the values in the process launching each service or in IDE run configurations. JWT_SECRET is Base64 text decoding to at least 32 bytes and must match in every service. INTERNAL_SERVICE_KEY is shared by Auth, Member, and Membership. DB_PASSWORD must match PostgreSQL. Four DB URL settings are *_DB_URL as listed in the sample. Gateway upstream settings are AUTH_SERVICE_URL, MEMBER_SERVICE_URL, MEMBERSHIP_SERVICE_URL, TRAINER_SERVICE_URL.

Initial admin setup is disabled by default. On a new database only, set ADMIN_SETUP_ENABLED=true and ADMIN_SETUP_SECRET, call POST http://localhost:8085/api/admin-setup/initial-admin, then disable the setting and restart Auth.

## Startup and shutdown

Start PostgreSQL first, then Member, Auth, Membership, Trainer, Gateway. Open one terminal per service at this folder and provide each process the environment above:

```powershell


.\mvnw.cmd spring-boot:run
```
```powershell
cd auth-service
.\mvnw.cmd spring-boot:run
```
```powershell
cd membership-service
.\mvnw.cmd spring-boot:run
```
```powershell
cd trainer-service
.\mvnw.cmd spring-boot:run
```
```powershell
cd api-gateway
.\mvnw.cmd spring-boot:run
```

Stop each with Ctrl+C, then PostgreSQL. Gateway readiness: GET http://localhost:8083/actuator/health. These commands are not a claim that all services have been live-verified together.

## Authentication and roles

Send Authorization: Bearer <JWT> for protected calls. Registration/login are public. JWTs use HS256 and expiry. Roles: ADMIN, MEMBER, TRAINER. Admin manages members, memberships, trainers. Members access their own profile/membership/payment data, enroll/renew/cancel, view plans and browse trainers. TRAINER can GET /api/trainers/me. Internal member endpoints require X-Internal-Service-Key; normal clients must not send it. Direct service ports also enforce authorization. Registration JSON has nested account and profile objects with matching emails; response accountId and memberId are distinct, and ownership uses memberId.

## API summary and Postman

Use base URL http://localhost:8083 and Bearer Token auth for protected requests. Send JSON for POST/PUT/PATCH.

Registration: POST /api/auth/register with:
```json
{"account":{"name":"Sample Member","email":"sample.member@example.com","password":"change-this-password"},"profile":{"name":"Sample Member","age":25,"mobile":"9000000001","gymTime":"Morning","email":"sample.member@example.com","gender":"Other","fatherName":"Parent","amount":0}}
```
Login: POST /api/auth/login with {"email":"...","password":"..."}.

Members: Admin GET /api/members?page=0&size=10&sortBy=id&direction=asc (Spring Page JSON), GET /api/members/{id}, POST /api/members, PUT/DELETE /api/members/{id}. Member self profile GET/PATCH /api/members/me. Page size is 1-100; self update excludes billing fields.

Memberships: GET /api/memberships/plans; admin GET /api/memberships (max 100), GET /api/memberships/page, GET /api/memberships/{id}, POST /api/memberships; PUT/DELETE /api/memberships/{id}, POST /api/memberships/plan?memberId={id}&plan=MONTHLY. Members use GET /api/memberships/me, POST /api/memberships/me and /me/renewals with {"plan":"MONTHLY"}, and POST /api/memberships/me/{id}/cancel. Plans: MONTHLY, QUARTERLY, YEARLY. Simulated payments: POST /api/memberships/me/{id}/payments with Idempotency-Key, then POST /api/memberships/me/payments/{paymentId}/simulate with {"outcome":"SUCCESS"} or FAILURE. No real provider/amount input is implemented.

Trainers: GET /api/trainers or GET /api/trainers/page?q=&specialization=&availability=&status=&page=0&size=20; GET /api/trainers/{id}; admin POST/PUT/DELETE /api/trainers. GET /api/trainers/me requires TRAINER token, but trainer account provisioning is absent.

## Testing and limits

Run .\mvnw.cmd test from each service. Spring context tests may connect to configured databases; use isolated test databases. Mock-based unit tests do not prove live inter-service communication. Use Postman through the gateway for that.

No attendance API/persistence exists. Payments are simulated. No versioned database migrations exist. Trainer account provisioning is absent. The frontend client currently does not attach bearer tokens; protected API calls will need a frontend login/token flow. Frontend files were not changed. Full service startup and live Postman workflows remain unverified.
