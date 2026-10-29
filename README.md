<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:4facfe,50:00f2fe,100:43e97b&height=260&section=header&text=Gym%20Management%20System&fontSize=45&fontColor=ffffff&animation=fadeIn&fontAlignY=40" width="100%" alt="Gym Management System Header"/>

# 🏋️ Gym Management System

### 💪 From Desktop Application to Microservices Architecture

<p align="center">
  <img src="https://img.shields.io/badge/Version-2.0%20In%20Development-00b894?style=for-the-badge&logo=github" alt="Version 2 in development"/>
  <img src="https://img.shields.io/badge/Java-Spring%20Boot-orange?style=for-the-badge&logo=openjdk" alt="Java Spring Boot"/>
  <img src="https://img.shields.io/badge/PostgreSQL-Database-4169E1?style=for-the-badge&logo=postgresql" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/REST%20API-Microservices-blueviolet?style=for-the-badge" alt="REST API Microservices"/>
  <img src="https://img.shields.io/badge/Version%201-Java%20Swing-blue?style=for-the-badge&logo=java" alt="Version 1 Java Swing"/>
</p>

<p align="center">
  <a href="https://github.com/Akshay001-A/Gym-Management-System"><img src="https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github" alt="GitHub Repository"/></a>
  <a href="https://github.com/Akshay001-A/Gym-Management-System/releases/tag/v1.0.0"><img src="https://img.shields.io/badge/Release-V1.0.0-blue?style=for-the-badge&logo=github" alt="Version 1 Release"/></a>
  <img src="https://img.shields.io/badge/Development-Active-success?style=for-the-badge" alt="Active Development"/>
</p>

<br/>

<img src="https://readme-typing-svg.herokuapp.com?font=Poppins&size=25&duration=3000&color=36BCF7&center=true&vCenter=true&width=900&lines=Gym+Management+System+V2;Spring+Boot+%2B+PostgreSQL+%2B+REST+APIs;Member+and+Payment+Services;Building+a+Scalable+Backend+Architecture;V1+Desktop+Application+Preserved" alt="Animated project introduction"/>

</div>

---

# 🌟 Project Overview

**Gym Management System** is an evolving software project designed to simplify gym administration, member management, and related operations.

The project began as a Java Swing desktop application and is being upgraded into a backend architecture using Spring Boot, REST APIs, and PostgreSQL. The original desktop application remains in the repository while the new services are developed.

### 🚀 Project Evolution

| Version | Architecture | Technology | Status |
|---|---|---|---|
| **V1.0.0** | Desktop application | Java Swing, JDBC, MySQL | Original release preserved |
| **V2.0** | Microservices-based backend | Spring Boot, REST APIs, PostgreSQL | In development |
| **V2 — Member Service** | Member management API | Spring Boot, Spring Data JPA, PostgreSQL | Implemented; tested locally |
| **V2 — Payment Service** | Payment API and gateway integration | Spring Boot, PostgreSQL, Razorpay, JWT | Implemented; integration and reliability testing ongoing |

**Current development focus:** building and validating independent backend services, beginning with member management and payment processing.

---

# 🚀 Version 2.0 — Microservices Architecture

V2 separates backend responsibilities into services that can be developed and maintained independently. The current repository contains the Member Service and a Payment Service under active development.

<div align="center">
  <img src="https://img.shields.io/badge/Member%20Service-Implemented-00b894?style=for-the-badge&logo=springboot" alt="Member Service implemented"/>
  <img src="https://img.shields.io/badge/Payment%20Service-In%20Development-f39c12?style=for-the-badge&logo=springboot" alt="Payment Service in development"/>
  <img src="https://img.shields.io/badge/Database-PostgreSQL-4169E1?style=for-the-badge&logo=postgresql" alt="PostgreSQL database"/>
  <img src="https://img.shields.io/badge/API-REST-blue?style=for-the-badge" alt="REST API"/>
</div>

## ✨ V2 Member Service Features

- ✅ Create, retrieve, update, and delete gym member records.
- ✅ Validate incoming request data.
- ✅ Check for duplicate email addresses and mobile numbers.
- ✅ Handle member-not-found and other API exceptions.
- ✅ Use PostgreSQL for persistent data storage.
- ✅ Support pagination and sorting when retrieving member records.
- ✅ Separate controller, service, repository, entity, and exception-handling responsibilities.

## 💳 V2 Payment Service Features

- ✅ Spring Boot REST API foundation for payment operations.
- ✅ PostgreSQL-backed payment records and webhook-event records.
- ✅ Razorpay order-creation integration using server-side credentials.
- ✅ Checkout payment-signature verification.
- ✅ JWT-based authentication for protected payment endpoints.
- ✅ Member-specific payment retrieval and paginated payment history.
- ✅ Idempotency-key support for payment-order requests.
- ✅ Razorpay webhook signature validation using the raw request body.
- ✅ Webhook event tracking and handling for `payment.captured` and `payment.failed` events.
- ✅ Unit tests for core webhook-service scenarios.
- ⚠️ End-to-end checkout, trusted payment-status reconciliation, webhook retry/recovery, and cross-service membership activation still require further validation.

> **Development status:** a passing local build or unit test does not mean the full payment journey has been verified against live infrastructure. Use Razorpay Test Mode until end-to-end and reliability tests are complete.

## 🛠 V2 Technology Stack

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-orange?style=for-the-badge&logo=openjdk" alt="Java 25"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?style=for-the-badge&logo=springboot" alt="Spring Boot 4.1.2"/>
  <img src="https://img.shields.io/badge/Spring%20Data%20JPA-ORM-6DB33F?style=for-the-badge&logo=spring" alt="Spring Data JPA"/>
  <img src="https://img.shields.io/badge/PostgreSQL-18-4169E1?style=for-the-badge&logo=postgresql" alt="PostgreSQL 18"/>
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven" alt="Maven"/>
</p>

| Category | Technology |
|---|---|
| Programming language | Java 25 |
| Backend framework | Spring Boot |
| API architecture | REST |
| Data access | Spring Data JPA |
| ORM | Hibernate |
| Database | PostgreSQL |
| Payment gateway | Razorpay (Test Mode for development) |
| Authentication | JWT / Spring Security |
| Build tool | Maven Wrapper |
| API testing | Postman or another REST client |
| Version control | Git and GitHub |
| IDE | IntelliJ IDEA |

## 🏗 V2 Architecture

```text
                         Client / API Consumer
                                  |
                                  v
                      +------------------------+
                      |   Member Service       |
                      |   REST API :8081       |
                      +------------------------+
                                  |
                                  v
                         PostgreSQL Database
                           gym_member_db

                      +------------------------+
                      |   Payment Service      |
                      |   REST API :8086       |
                      +------------------------+
                         |                |
                         v                v
                 PostgreSQL DB       Razorpay API
                 gym_payment_db      (Test Mode)

             Payment Service also exposes a webhook endpoint
             for verified Razorpay payment events.
```

The services are developed separately. Service-to-service workflows, membership activation after successful payment, and production deployment are still being completed and validated.

### 📂 V2 Project Structure

```text
microservices/
├── member-service/
│   ├── src/
│   │   ├── main/java/
│   │   └── main/resources/application.yaml
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
├── payment-service/
│   ├── src/
│   │   ├── main/java/com/gym/payment/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── main/resources/application.yaml
│   │   └── test/java/com/gym/payment/
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
├── .env.example
└── load-env.ps1
```

*This is a simplified overview; additional files may be present in each service.*

## 🔗 V2 API Endpoints

### Member Service

The Member Service is configured to run locally on port `8081`.

Base URL: `http://localhost:8081/api/members`

| HTTP Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/members` | Create a member |
| `GET` | `/api/members` | Retrieve members, with pagination/sorting where configured |
| `GET` | `/api/members/{id}` | Retrieve a member by ID |
| `PUT` | `/api/members/{id}` | Update a member |
| `DELETE` | `/api/members/{id}` | Delete a member |

Pagination example:

```http
GET http://localhost:8081/api/members?page=0&size=2
```

### Payment Service

The Payment Service is configured to run locally on port `8086`. Protected routes require a valid JWT. The Razorpay webhook route is public at the HTTP-security layer but verifies the Razorpay signature before accepting an event.

Base URL: `http://localhost:8086`

| HTTP Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/payments/orders` | Create a payment order for an authenticated member |
| `POST` | `/api/payments/verify` | Verify checkout payment details/signature |
| `GET` | `/api/payments/{paymentId}` | Retrieve the authenticated member's payment |
| `GET` | `/api/payments/history` | Retrieve paginated payment history for the authenticated member |
| `POST` | `/api/payments/webhooks/razorpay` | Receive Razorpay webhook events after signature validation |
| `GET` | `/actuator/health` | Check service health |

Example health check:

```http
GET http://localhost:8086/actuator/health
```

> These endpoints describe the current local service configuration. They do not imply that the complete platform is publicly deployed or that the payment flow is production-ready.

---

# ⚙️ Running V2 Locally

## 📋 Prerequisites

Install and configure:

- Java 25 (or a compatible Java version matching the project configuration).
- PostgreSQL 18 or a compatible PostgreSQL version.
- Git.
- PowerShell on Windows, or a compatible terminal.
- Postman or another REST API client.
- Razorpay Test Mode credentials for payment-service testing.

## 📥 Step 1 — Clone the Repository

```bash
git clone https://github.com/Akshay001-A/Gym-Management-System.git
cd Gym-Management-System
```

## 🧩 Step 2 — Choose a Service

For the Member Service:

```powershell
cd microservices/member-service
```

For the Payment Service, open a separate terminal from the repository root:

```powershell
cd microservices/payment-service
```

## 🗄 Step 3 — Configure PostgreSQL

Create the database used by the Member Service:

```sql
CREATE DATABASE gym_member_db;
```

Create the database used by the Payment Service:

```sql
CREATE DATABASE gym_payment_db;
```

Configure database usernames and passwords locally. Do not put real credentials in source-controlled files.

### Member Service environment variables

The exact variable names should match the Member Service's `application.yaml`. For example, if it uses `DB_USERNAME` and `DB_PASSWORD`, set them in the current PowerShell session:

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "YOUR_LOCAL_DATABASE_PASSWORD"
```

Replace the placeholder with your local password. These variables apply only to the current terminal session.

### Payment Service environment configuration

The repository's local setup uses `microservices/.env` and the PowerShell helper `microservices/load-env.ps1`. Keep the actual `.env` file private and use `.env.example` as a template only.

From the `microservices/payment-service` directory, load the variables for the current PowerShell session:

```powershell
. ..\load-env.ps1
```

Confirm that the required variable names are configured without printing their secret values. The payment service expects configuration for:

- `PAYMENT_DB_URL` (defaults to `jdbc:postgresql://localhost:5432/gym_payment_db` if not set)
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `RAZORPAY_WEBHOOK_SECRET`
- `MEMBERSHIP_SERVICE_URL` (defaults to `http://localhost:8082`)

Use your own local values. Never commit API secrets, JWT signing secrets, webhook secrets, or database passwords. Do not paste them into issues or public logs.

## ▶️ Step 4 — Start the Service

### Start the Member Service

From `microservices/member-service`:

```powershell
.\mvnw.cmd spring-boot:run
```

The configured local base URL is `http://localhost:8081`.

### Start the Payment Service

From `microservices/payment-service`, after loading the environment variables:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

The configured local base URL is `http://localhost:8086`.

Keep the application terminal running while testing the service. If the Payment Service needs the Membership Service for trusted plan pricing, ensure that service is running at the configured `MEMBERSHIP_SERVICE_URL`.

## 🧪 Step 5 — Test the APIs

Member Service example:

```http
GET http://localhost:8081/api/members?page=0&size=2
```

Payment Service health check:

```http
GET http://localhost:8086/actuator/health
```

Payment creation, verification, and history endpoints require a valid JWT and valid request data. Use Razorpay Test Mode credentials and test cards only while developing. A webhook request without a valid `X-Razorpay-Signature` should be rejected; do not treat a manually posted unsigned event as a successful payment test.

---

# 🖥 Version 1.0.0 — Original Desktop Application

V1 is the original Gym Management System, developed using **Java Swing, JDBC, and MySQL**. Its implementation and release history are preserved in this repository.

## ✨ V1 Features

### 👤 Member Features

- ✅ Secure member login.
- ✅ Member dashboard.
- ✅ View membership information.
- ✅ Access workout schedules.
- ✅ View payment information.
- ✅ Receive notifications.

### 🛠 Admin Features

- ✅ Add new members.
- ✅ Update member information.
- ✅ Delete member records.
- ✅ View registered members.
- ✅ Manage membership payments.
- ✅ Manage gym records.
- ✅ Access the admin dashboard.

## 🗄 V1 Database Integration

The desktop application uses MySQL and JDBC to store and retrieve gym-related information, including member records, payment information, workout schedules, and notifications.

## 📸 V1 Application Screenshots

### 👤 Member Dashboard

Interactive dashboard for members to view gym-related information.

<img src="https://github.com/Akshay001-A/Gym-Management-System/blob/main/assets/customerdash.png?raw=true" width="100%" alt="V1 Member Dashboard"/>

### 🛠 Admin Dashboard

Administrative interface for managing gym operations.

<img src="https://github.com/Akshay001-A/Gym-Management-System/blob/main/assets/admindash.png?raw=true" width="100%" alt="V1 Admin Dashboard"/>

## 🛠 V1 Technology Stack

| Category | Technology |
|---|---|
| Programming language | Java |
| GUI framework | Java Swing |
| Database | MySQL |
| Database connectivity | JDBC |
| IDE | Eclipse IDE |
| Version control | Git and GitHub |

## ▶️ Running V1 Locally

1. Clone the repository using the instructions above.
2. Open the original desktop project in Eclipse IDE.
3. Configure MySQL and initialize the database using the SQL script supplied with the project.
4. Update the local database connection settings in the existing source code.
5. Run the application's entry-point class.

The original desktop application's database setup and configuration are separate from the V2 PostgreSQL setup.

---

# 📂 Repository Structure

```text
Gym-Management-System/
├── src/                         # V1 desktop source code
├── assets/                      # V1 images and icons
├── bin/                         # Existing project files
├── .settings/                   # Existing Eclipse settings
├── microservices/
│   ├── member-service/          # V2 Member Service
│   ├── payment-service/         # V2 Payment Service
│   ├── .env.example             # Safe environment template
│   └── load-env.ps1             # Local environment loader
├── .gitignore
├── LICENSE
└── README.md
```

*This is a simplified overview; additional files from the original project and services may also be present.*

---

# 🔐 Security and Configuration

- `.env` files containing local secrets should be excluded from version control.
- Maven `target/` directories should be excluded.
- IDE-specific files should be excluded according to repository ignore rules.
- Database passwords and API credentials must be configured locally.
- Razorpay webhook signatures must be validated against the unmodified raw request body.
- Keep Razorpay keys in Test Mode for development; rotate any credential that is accidentally exposed.

Never publish database credentials, access tokens, JWT signing secrets, Razorpay API secrets, or webhook secrets in source code, README files, screenshots, or logs.

---

# 🛣 Development Roadmap

The following roadmap distinguishes existing foundations from work still required. Completion of individual service code does not imply full end-to-end or production readiness.

- [x] Preserve the original V1 desktop application.
- [x] Build the Member Service REST API.
- [x] Integrate PostgreSQL with Spring Boot for the Member Service.
- [x] Implement member CRUD operations, validation, exception handling, pagination, and sorting.
- [x] Add the Payment Service foundation and PostgreSQL payment records.
- [x] Add Razorpay order creation and checkout-signature verification code.
- [x] Add JWT-protected payment endpoints and webhook-signature validation code.
- [x] Add unit tests for core webhook-service scenarios.
- [ ] Complete real Razorpay Test Mode checkout and verification tests.
- [ ] Improve webhook transaction boundaries, duplicate-event handling, and retry/recovery behavior.
- [ ] Reconcile payment status with trusted gateway status before considering payment final.
- [ ] Integrate successful payment with membership activation exactly once.
- [ ] Add integration tests against PostgreSQL and service dependencies.
- [ ] Complete cross-service authentication and authorization validation.
- [ ] Add Docker-based development and deployment configuration.
- [ ] Build and integrate the V2 frontend.
- [ ] Deploy the application to a suitable hosting platform.
- [ ] Add production database migrations, monitoring, and operational safeguards.

---

# 🌱 Future Improvements

Potential enhancements for the overall Gym Management System include:

- ☁ Cloud deployment.
- 📱 Mobile application integration.
- 📊 Advanced analytics dashboard.
- 💳 Complete online payment and membership lifecycle.
- 📈 Membership and revenue reports.
- 🔔 Email and SMS notifications.
- 🧠 AI-based fitness recommendations.
- 🔐 Improved authentication and authorization.
- 🐳 Containerized deployment.
- ⚙ Automated CI/CD pipelines.

---

# 👨‍💻 Developed By

<div align="center">

## Akshay R 🚀

### Full Stack Developer | Java Developer | AI Enthusiast

<p align="center">
  <a href="https://github.com/Akshay001-A"><img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github" alt="GitHub"/></a>
  <a href="https://www.linkedin.com/in/akshayofficial0207"><img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin" alt="LinkedIn"/></a>
  <a href="https://www.instagram.com/akshay_authentic"><img src="https://img.shields.io/badge/Instagram-E4405F?style=for-the-badge&logo=instagram" alt="Instagram"/></a>
</p>

</div>

---

## 🤝 Contributors

- **Akshay R** — Project development and integration.

---

# 📄 License

This project is licensed under the **MIT License**.

See the [`LICENSE`](LICENSE) file for details.

---

<div align="center">

## ⭐ If You Like This Project, Give It a Star! ⭐

<img src="https://readme-typing-svg.herokuapp.com?font=Poppins&size=23&duration=3000&color=00F7FF&center=true&vCenter=true&width=850&lines=From+Desktop+Application+to+Microservices;Spring+Boot+%26+PostgreSQL;Building+a+Scalable+Gym+Management+System" alt="Animated project closing message"/>

**V1 preserved. V2 in development. One evolving project.**

</div>
