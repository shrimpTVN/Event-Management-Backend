# 🎓 University Event & Student Training Point Management System (Backend)

> An enterprise-grade backend built with **Java 21**, **Spring Boot**, and **Domain-Driven Design (DDD)** to streamline university event governance, club management, automated photo check-in, and student training point (*Điểm rèn luyện*) evaluation.

---

## 📌 Overview

This platform bridges the gap between university event organizers (Student Affairs, Faculties, Student Clubs) and students. It automates the full lifecycle of campus activities—from two-step event moderation and seat reservation to photo-verified self check-in and automated calculation of academic training criteria progress according to university regulations.

---

## 🌟 Key Features

### 🔐 Authentication & Identity Governance
- **Google OAuth2 & Email Auth**: Seamless sign-in supporting university Google Workspace accounts and standard credentials.
- **Stateless JWT with Token Rotation**: Secure session management featuring refresh token rotation and instant token revocation on logout.
- **Fine-Grained RBAC**: Multi-role support (`ADMIN`, `ORGANIZER`, `STUDENT`, `FANPAGE_ADMIN`, `FANPAGE_MEMBER`).

### 📅 Event Lifecycle & Two-Step Moderation
- **Moderation Workflow**: Dual-stage approval process (Draft → Pending Review → Approved / Rejected → Published) ensuring administrative oversight.
- **Capacity & Registration Control**: Dynamic seat caps, registration windows, and attendee roster management.
- **Criteria Association**: Direct binding between specific events and university evaluation criteria.

### 📸 Self Check-In & Attendance Verification
- **Photo-Based Self Check-In**: Students submit self-check-in photos upon event arrival for auditability and fraud prevention.
- **Attendance State Machine**: Real-time status transitions (`REGISTERED` → `APPROVED` → `ATTENDED` / `ABSENT`).

### 📈 Training Points & Academic Progress Tracking (*Đánh giá Rèn luyện*)
- **Hierarchical Criteria Evaluation**: Multi-level criteria tree aligned with university evaluation standards.
- **Automated Point Ledger**: Verified event check-ins trigger point transactions credited directly to the student's record.
- **Semester & Academic Year Rollup**: Real-time progress calculation per semester and academic year.

### 👥 Clubs & Fanpages Management
- **Organization Profiles**: Dedicated spaces for university clubs, associations, and faculty boards.
- **Member Invitation System**: Complete invitation lifecycle (Pending, Accepted, Rejected) with role assignment.

### 🔔 Real-Time Notifications & Auditing
- **Server-Sent Events (SSE)**: Streaming real-time updates for approvals, registration status changes, and announcements.
- **Automatic JPA Auditing**: Comprehensive audit trail tracking `createdBy`, `updatedBy`, and timestamps across all business records.

---

## 🏗️ Architecture & Design Principles

This project is built around **Domain-Driven Design (DDD)** and **Hexagonal Architecture (Ports & Adapters)** to decouple business logic from external frameworks, databases, and delivery mechanisms.

### Multi-Module Structure

```
ddd/
├── ddd-domain         # Pure Java domain models, business exceptions, repository & port interfaces (ZERO frameworks)
├── ddd-infrastructure # JPA entity implementations, Spring Security adapters, Cloudinary, and external services
├── ddd-application    # CQRS services (*CommandService / *QueryService), DTOs, mappers, and domain workflows
├── ddd-api            # REST controllers, API request/response records, and global exception translation
└── ddd-start          # Spring Boot bootstrap, configuration roots, and JPA auditing enablement
```

### Allowed Dependency Flow

```
ddd-api  ──▶  ddd-application  ──▶  ddd-domain
                    │                   ▲
                    ▼                   │
            ddd-infrastructure  ────────┘
                    ▲
                ddd-start
```

### Key Architectural Guidelines
- **CQRS (Command Query Responsibility Segregation)**:
  - **Command Services** (`*CommandService`): Handle write operations and state changes inside write transactions (`@Transactional`).
  - **Query Services** (`*QueryService`): Dedicated to data retrieval inside read-only transactions (`@Transactional(readOnly = true)`).
- **Zero-Framework Domain Core**: The `ddd-domain` layer contains pure POJOs without Spring or JPA annotations, ensuring domain rules remain portable and testable.
- **Decoupled Identity Resolution**: Controllers never pass user IDs or email parameters into business services. Instead, application services resolve identity directly via `SecurityContextPort`.
- **Standardized API Envelope**: All REST endpoints return a unified `BaseResponse<T>` wrapper with consistent HTTP status codes and error payloads.

---

## 🛠️ Tech Stack

| Category | Technology | Purpose |
|---|---|---|
| **Language & Platform** | Java 21 | Modern LTS Java runtime (Records, Pattern matching) |
| **Framework** | Spring Boot 4.x / 3.x | Enterprise application foundation |
| **Security** | Spring Security 6 & OAuth2 Client | JWT authentication, Google OAuth2 login, RBAC filters |
| **Database** | PostgreSQL | Primary relational database |
| **ORM / Data Access** | Spring Data JPA / Hibernate | Object-relational mapping, custom JPQL queries, and Auditing |
| **Object Mapping** | MapStruct 1.5.5 | Type-safe compile-time bean mapping across DDD boundaries |
| **Media Storage** | Cloudinary SDK | Cloud storage for check-in proof photos and club media |
| **Real-time Streaming** | Spring Web MVC (SSE) | Server-Sent Events for real-time notification push |
| **DevOps & Containers** | Docker & Docker Compose | Containerized PostgreSQL environment |
| **Build Tool** | Apache Maven | Multi-module project lifecycle management |

---

## 🚀 Getting Started

### Prerequisites
- **JDK 21** installed and configured in your environment
- **Docker & Docker Compose** for database provisioning
- **Maven** (or use the included `./mvnw` wrapper)

### 1. Environment Configuration
Copy the sample environment file to `.env`:
```bash
cp .env.example .env
```
Ensure required credentials in `.env` are configured (Database, Google OAuth2, Cloudinary, JWT Secret).

### 2. Start PostgreSQL Database
```bash
docker compose up -d
```

### 3. Build the Project
```bash
./mvnw clean install
```

### 4. Run the Application
Launch the application via the entry point module (`ddd-start`):
```bash
./mvnw spring-boot:run -pl ddd-start
```

The application will be accessible at `http://localhost:8080`.

---

## 📖 Documentation & References

- [Architecture & Coding Conventions](architecture_conventions.md)
- [API Documentation](docs/api_documentation.md)
- [Domain Enums Documentation](docs/domain_enums_documentation.md)
- [Security Analysis](docs/security_analysis.md)
