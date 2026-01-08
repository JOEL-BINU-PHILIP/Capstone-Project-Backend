
# Service Management System – Backend

A **Spring Boot Microservices–based Service Management System** designed to manage authentication, service catalog, bookings, billing, and notifications using a scalable, event-driven architecture.

---

##  Tech Stack

* **Backend:** Java 17, Spring Boot 3.2
* **Microservices:** Spring Cloud (Eureka, Config Server, Gateway)
* **Database:** MongoDB
* **Messaging:** RabbitMQ
* **Security:** Spring Security, JWT
* **Build Tool:** Maven (Multi-module)
* **Containerization:** Docker & Docker Compose
* **Testing:** JUnit 5, Mockito, JaCoCo

---

## Microservices Overview

| Service              | Description                                 | Port            |
| -------------------- | ------------------------------------------- | --------------- |
| API Gateway          | Entry point for all client requests         | `8080`          |
| Auth Service         | Authentication, JWT, user & role management | `8081`          |
| Service Catalog      | Manage service categories & items           | `8082`          |
| Booking Service      | Service booking & dashboard reports         | `8083`          |
| Billing Service      | Invoicing & payments                        | `8084`          |
| Notification Service | Email & event-based notifications           | `8085`          |
| Service Registry     | Eureka service discovery                    | `8761`          |
| Config Server        | Centralized configuration                   | `8888`          |
| MongoDB              | NoSQL database                              | `27017`         |
| RabbitMQ             | Event messaging                             | `5672`, `15672` |

---

## Project Structure

```text
capstone-service-management-system/
├── api-gateway
├── auth-service
├── service_catalog
├── booking-service
├── billing-service
├── notification-service
├── service-registry
├── docker-compose.yml
├── pom.xml
```

📌 This is a **multi-module Maven project** with a shared parent POM 

---

## 🚀 Running the Application (Docker – Recommended)

### 1 Prerequisites

* Docker
* Docker Compose
* Java 17 (only if running locally without Docker)

---

### 2️ Build all services

```bash
mvn clean package 
```

---

### 3️ Start all services

```bash
docker compose up -d
```

This will start:

* MongoDB
* RabbitMQ
* Config Server
* Eureka
* All microservices
* API Gateway

---

###  Verify services

| URL                                                          | Purpose          |
| ------------------------------------------------------------ | ---------------- |
| [http://localhost:8080](http://localhost:8080)               | API Gateway      |
| [http://localhost:8761](http://localhost:8761)               | Eureka Dashboard |
| [http://localhost:15672](http://localhost:15672)             | RabbitMQ UI      |
| [http://localhost:8080/health](http://localhost:8080/health) | Gateway Health   |

---

##  Default Users (Auto-Created)

| Role            | Username          | Password      |
| --------------- | ----------------- | ------------- |
| Admin           | `admin`           | `Admin@123`   |
| Service Manager | `service_manager` | `Manager@123` |

> Created automatically on Auth Service startup.

---

##  Authentication Flow

1. Login via **Auth Service**
2. Receive **JWT Access + Refresh Token**
3. All protected APIs require:

   ```
   Authorization: Bearer <JWT>
   ```
4. Token refresh supported

---

## Inter-Service Communication

* **Synchronous:** OpenFeign REST clients
* **Asynchronous:** RabbitMQ events
* **Service Discovery:** Eureka
* **Centralized Config:** Spring Cloud Config Server

---

## Testing & Code Coverage

### Run Tests

```bash
mvn test
```

### Generate JaCoCo Report

```bash
mvn verify
```

Coverage configured for:

* Controllers
* Service Implementations

---

## Database Access (MongoDB in Docker)

### From Host Machine

```text
mongodb://localhost:27017
```

### From Another Container

```text
mongodb://mongo-db:27017
```

### Enter Mongo Shell

```bash
docker exec -it mongo-db mongosh
```

---

## ⚙️ Environment Profiles

| Profile   | Usage                    |
| --------- | ------------------------ |
| `default` | Docker / production-like |
| `test`    | Unit & integration tests |

---

## Key Features

* JWT-based authentication & role-based access
* Event-driven billing & notification flow
* Audit logs & rate limiting
* Secure internal APIs
* Centralized config & service discovery
* Docker-first deployment

---


