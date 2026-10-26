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
  <a href="https://github.com/Akshay001-A/Gym-Management-System">
    <img src="https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github" alt="GitHub Repository"/>
  </a>
  <a href="https://github.com/Akshay001-A/Gym-Management-System/releases/tag/v1.0.0">
    <img src="https://img.shields.io/badge/Release-V1.0.0-blue?style=for-the-badge&logo=github" alt="Version 1 Release"/>
  </a>
  <img src="https://img.shields.io/badge/Development-Active-success?style=for-the-badge" alt="Active Development"/>
</p>

<br/>

<img src="https://readme-typing-svg.herokuapp.com?font=Poppins&size=25&duration=3000&color=36BCF7&center=true&vCenter=true&width=900&lines=Gym+Management+System+V2;Spring+Boot+%2B+PostgreSQL+%2B+REST+APIs;Member+Management+Microservice;Building+a+Scalable+Backend+Architecture;V1+Desktop+Application+Preserved" alt="Animated project introduction"/>

</div>

---

# 🌟 Project Overview

**Gym Management System** is an evolving software project designed to simplify gym administration, member management, and related operations.

The project began as a Java Swing desktop application and is now being developed into a modern backend architecture using Spring Boot and PostgreSQL.

The repository preserves the original desktop application while introducing a new microservices-based implementation.

### 🚀 Project Evolution

| Version | Architecture | Technology | Status |
|---|---|---|---|
| **V1.0.0** | Desktop application | Java Swing, JDBC, MySQL | Original release preserved |
| **V2.0** | Microservices-based backend | Spring Boot, REST APIs, PostgreSQL | In development |
| **V2 — Member Service** | Member management API | Spring Boot, Spring Data JPA, PostgreSQL | Implemented and tested locally |

**Current development focus:** V2 — building the backend services required for a scalable Gym Management System.

---

# 🚀 Version 2.0 — Microservices Architecture

V2 introduces a backend-oriented approach in which gym management functionality can be organized into independently developed services.

The first implemented component is the **Member Service**, which provides REST APIs for managing gym members.

<div align="center">

<img src="https://img.shields.io/badge/Member%20Service-Implemented-00b894?style=for-the-badge&logo=springboot" alt="Member Service implemented"/>
<img src="https://img.shields.io/badge/Database-PostgreSQL-4169E1?style=for-the-badge&logo=postgresql" alt="PostgreSQL database"/>
<img src="https://img.shields.io/badge/API-REST-blue?style=for-the-badge" alt="REST API"/>

</div>

## ✨ V2 Member Service Features

- ✅ Create new gym member records.
- ✅ Retrieve member information.
- ✅ Update existing member records.
- ✅ Delete member records.
- ✅ Validate incoming request data.
- ✅ Check for duplicate email addresses and mobile numbers.
- ✅ Handle member-not-found and other API exceptions.
- ✅ Use PostgreSQL for persistent data storage.
- ✅ Support pagination and sorting when retrieving member records.
- ✅ Separate controller, service, repository, entity, and exception-handling responsibilities.

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
| Programming language | Java |
| Backend framework | Spring Boot |
| API architecture | REST |
| Data access | Spring Data JPA |
| ORM | Hibernate |
| Database | PostgreSQL |
| Build tool | Maven |
| API testing | Postman |
| Version control | Git and GitHub |
| IDE | IntelliJ IDEA |

## 🏗 V2 Architecture

```text
                  Client / API Consumer
                           |
                           v
                REST API - Port 8081
                           |
                           v
                  MemberController
                           |
                           v
                    MemberService
                           |
                           v
                  MemberRepository
                           |
                           v
                     PostgreSQL
                    gym_member_db
```

### 📂 V2 Project Structure

```text
microservices/
└── member-service/
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   │   └── com/gym/memberservice/
    │   │   │       ├── controller/
    │   │   │       │   └── MemberController.java
    │   │   │       ├── entity/
    │   │   │       │   └── Member.java
    │   │   │       ├── exception/
    │   │   │       ├── repository/
    │   │   │       │   └── MemberRepository.java
    │   │   │       ├── service/
    │   │   │       │   └── MemberService.java
    │   │   │       └── MemberServiceApplication.java
    │   │   └── resources/
    │   │       └── application.yaml
    │   └── test/
    ├── .gitignore
    ├── mvnw
    ├── mvnw.cmd
    └── pom.xml
```

## 🔗 V2 API Endpoints

The Member Service is configured to run locally on port `8081`.

Base URL:

```text
http://localhost:8081/api/members
```

| HTTP Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/members` | Create a member |
| `GET` | `/api/members` | Retrieve members |
| `GET` | `/api/members/{id}` | Retrieve a member by ID |
| `PUT` | `/api/members/{id}` | Update a member |
| `DELETE` | `/api/members/{id}` | Delete a member |

### 📄 Pagination and Sorting Example

```http
GET http://localhost:8081/api/members?page=0&size=2
```

This request retrieves the first page of members, with a page size of two.

> The endpoints above describe the local Member Service API. They do not imply that V2 has already been deployed publicly.

---

# ⚙️ Running V2 Locally

## 📋 Prerequisites

Install and configure the following:

- Java compatible with the project configuration.
- PostgreSQL.
- Git.
- Postman, or another REST API client.

The project was developed using Java 25 and Spring Boot 4.1.2. Use a compatible Java installation to run it.

## 📥 Step 1 — Clone the Repository

```bash
git clone https://github.com/Akshay001-A/Gym-Management-System.git
```

Navigate to the repository:

```bash
cd Gym-Management-System
```

## 📂 Step 2 — Navigate to the Member Service

```bash
cd microservices/member-service
```

## 🗄 Step 3 — Configure PostgreSQL

Create the database using PostgreSQL:

```sql
CREATE DATABASE gym_member_db;
```

Configure the database username and password for your local environment.

### Windows PowerShell

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "YOUR_LOCAL_DATABASE_PASSWORD"
```

Replace `YOUR_LOCAL_DATABASE_PASSWORD` with your actual local PostgreSQL password.

These environment variables apply to the current PowerShell session. Set them again if you open a new terminal.

**Security:** Do not commit real database credentials or passwords to GitHub.

## ▶️ Step 4 — Start the Member Service

From the `microservices/member-service` directory, run:

```powershell
.\mvnw.cmd spring-boot:run
```

The application should start on:

```text
http://localhost:8081
```

Make sure PostgreSQL is running and the database credentials are correct before starting the service.

## 🧪 Step 5 — Test the API

Open Postman and send:

```http
GET http://localhost:8081/api/members?page=0&size=2
```

You can also test the create, update, retrieve-by-ID, and delete operations using the appropriate HTTP methods and request bodies.

---

# 🖥 Version 1.0.0 — Original Desktop Application

V1 is the original Gym Management System, developed using **Java Swing, JDBC, and MySQL**.

The existing desktop implementation and its release history are preserved in this repository.

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

The desktop application uses MySQL and JDBC to store and retrieve gym-related information, including:

- Member records.
- Payment information.
- Workout schedules.
- Notifications.

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
│
├── src/                              # V1 desktop source code
├── assets/                           # V1 images and icons
├── bin/                              # Existing project files
├── .settings/                        # Existing Eclipse settings
│
├── microservices/
│   └── member-service/               # V2 Spring Boot service
│       ├── src/
│       ├── pom.xml
│       ├── mvnw
│       └── mvnw.cmd
│
├── .gitignore
├── LICENSE
└── README.md
```

*This is a simplified overview; additional files from the original project and service may also be present.*

---

# 🔐 Security and Configuration

The repository uses Git ignore rules to help prevent local secrets and generated files from being committed.

- `.env` files are excluded from version control.
- Maven `target/` directories are excluded.
- IDE-specific files are excluded according to the repository's ignore rules.
- Database passwords must be configured locally.

Never publish real database credentials, access tokens, or other secrets in source code or configuration files.

---

# 🛣 Development Roadmap

The following are potential next steps for V2. They are planned improvements, not features claimed to be completed.

- [x] Build the Member Service REST API.
- [x] Integrate PostgreSQL with Spring Boot.
- [x] Implement member CRUD operations.
- [x] Add validation and exception handling.
- [x] Add pagination and sorting.
- [x] Publish the Member Service source code to GitHub.
- [ ] Improve project documentation and setup instructions.
- [ ] Implement a Membership Service.
- [ ] Implement a Payment Service.
- [ ] Integrate services where required.
- [ ] Add automated testing across services.
- [ ] Add Docker-based development and deployment.
- [ ] Implement authentication and authorization.
- [ ] Build a frontend for the new backend.
- [ ] Deploy the application to a suitable hosting platform.
- [ ] Add monitoring and production-ready configuration.

---

# 🌱 Future Improvements

Potential enhancements for the overall Gym Management System include:

- ☁ Cloud deployment.
- 📱 Mobile application integration.
- 📊 Advanced analytics dashboard.
- 💳 Online payment gateway integration.
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
  <a href="https://github.com/Akshay001-A">
    <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github" alt="GitHub"/>
  </a>
  <a href="https://www.linkedin.com/in/akshayofficial0207">
    <img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin" alt="LinkedIn"/>
  </a>
  <a href="https://www.instagram.com/akshay_authentic">
    <img src="https://img.shields.io/badge/Instagram-E4405F?style=for-the-badge&logo=instagram" alt="Instagram"/>
  </a>
</p>

</div>

---

## 🤝 Contributors

- **Akshay R** — Project development and integration.
- **Charan Kumar R** — Contributor to the original project.

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