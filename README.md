# Eagle Bank – Core Engine API

A RESTful banking API built with **Spring Boot** that allows users to manage bank accounts and transactions. The API conforms to an OpenAPI 3.1 specification and is secured with JWT-based authentication.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Security](#security)
- [API Endpoints](#api-endpoints)
  - [Authentication](#authentication)
  - [Users](#users)
  - [Accounts](#accounts)
  - [Transactions](#transactions)
- [Error Responses](#error-responses)

---

## Tech Stack

| Layer            | Technology                             |
|------------------|----------------------------------------|
| Language         | Java 25                                |
| Framework        | Spring Boot 4.0.5                      |
| Security         | Spring Security + JWT (JJWT 0.12.7)   |
| Persistence      | Spring Data JPA + PostgreSQL           |
| API Spec         | OpenAPI 3.1 (code-generated via openapi-generator-maven-plugin) |
| API Docs UI      | SpringDoc OpenAPI (Swagger UI)         |
| Build Tool       | Maven                                  |

---

## Prerequisites

- **Java 25+**
- **Maven 3.9+**
- **Docker & Docker Compose** (for PostgreSQL)

---

## Getting Started

### 1. Start the database

```bash
docker compose up -d
```

This starts a PostgreSQL instance on the port configured in your `.env` file.

### 2. Build and run the application

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080` by default.

### 3. Swagger UI

Interactive API documentation is available at:

```
http://localhost:8080/swagger-ui
```

---

## Security

### Overview

The API uses **stateless JWT (JSON Web Token) authentication** enforced by Spring Security. There are no server-side sessions.

### How it works

1. **Register** – Create a user via `POST /v1/users` (public endpoint, no token required).
2. **Login** – Authenticate via `POST /auth/login` with your username and password. A signed JWT is returned.
3. **Authorised requests** – Include the JWT in the `Authorization` header of every subsequent request:
   ```
   Authorization: Bearer <your-token>
   ```

### Token details

| Property         | Value / Behaviour                                                    |
|------------------|----------------------------------------------------------------------|
| Algorithm        | HMAC-SHA (key derived from a Base64-encoded secret)                  |
| Claims           | `sub` = user ID, `username`, `iat`, `exp`                            |
| Default expiry   | **1 hour** (3 600 000 ms) — configurable via `app.security.jwt.expiration-ms` |
| Secret           | Configurable via `app.security.jwt.secret` (env / application config)|

### Password storage

User passwords are hashed with **BCrypt** before being stored in the database. Plaintext passwords are never persisted.

### Public endpoints (no token required)

| Method | Path          | Description          |
|--------|---------------|----------------------|
| POST   | `/auth/login` | Obtain a JWT token   |
| POST   | `/v1/users`   | Register a new user  |

All other endpoints require a valid Bearer token.

### Authorisation

Beyond authentication, the service enforces **ownership checks** — a user can only read or modify their own resources (accounts, transactions, profile). Attempting to access another user's data returns `403 Forbidden`.

### Configuration

The following properties can be overridden via environment variables or `application.yaml`:

| Property                            | Default                        | Description                      |
|-------------------------------------|--------------------------------|----------------------------------|
| `app.security.jwt.secret`           | *(built-in Base64 secret)*     | HMAC signing key (Base64-encoded)|
| `app.security.jwt.expiration-ms`    | `3600000`                      | Token lifetime in milliseconds   |
| `SPRING_DATASOURCE_URL`             | `jdbc:postgresql://localhost:5432/bank` | Database URL            |
| `DB_USER`                           | `bankuser`                     | Database username                |
| `DB_PASSWORD`                       | `bank_password`                | Database password                |

> ⚠️ **Production note:** Replace the default JWT secret with a strong, randomly generated key and supply it via a secrets manager or environment variable.

---

## API Endpoints

Base URL: `http://localhost:8080`

All protected endpoints require the header:
```
Authorization: Bearer <JWT>
```

---

### Authentication

#### `POST /auth/login`

Authenticate and receive a JWT token.

- **Auth required:** No

**Request body:**
```json
{
  "username": "testuser",
  "password": "s3cr3t"
}
```

**Responses:**

| Status | Description                    |
|--------|--------------------------------|
| `200`  | Login successful — returns JWT |
| `401`  | Invalid username or password   |
| `500`  | Unexpected server error        |

**Success response (`200`):**
```json
{
  "userId": "usr-abc123",
  "message": "Login successful",
  "token": "<JWT>"
}
```

---

### Users

#### `POST /v1/users`

Register a new user.

- **Auth required:** No

**Request body:**
```json
{
  "name": "Jane Doe",
  "username": "janedoe",
  "password": "s3cr3t",
  "address": {
    "line1": "1 High Street",
    "line2": "",
    "line3": "",
    "town": "London",
    "county": "Greater London",
    "postcode": "EC1A 1BB"
  },
  "phoneNumber": "+441234567890",
  "email": "jane@example.com"
}
```

**Responses:**

| Status | Description                              |
|--------|------------------------------------------|
| `201`  | User created successfully                |
| `400`  | Invalid request body                     |
| `409`  | Username already exists                  |
| `500`  | Unexpected server error                  |

---

#### `GET /v1/users/{userId}`

Fetch a user by their ID.

- **Auth required:** Yes
- **Path params:** `userId` — format `usr-[A-Za-z0-9]+`

**Responses:**

| Status | Description                              |
|--------|------------------------------------------|
| `200`  | User details returned                    |
| `400`  | Invalid request                          |
| `401`  | Missing or invalid token                 |
| `403`  | Not allowed to access this user          |
| `404`  | User not found                           |
| `500`  | Unexpected server error                  |

---

#### `PATCH /v1/users/{userId}`

Update a user's details.

- **Auth required:** Yes
- **Path params:** `userId` — format `usr-[A-Za-z0-9]+`

**Request body** (all fields optional):
```json
{
  "name": "Jane Smith",
  "username": "janesmith",
  "password": "newpassword",
  "address": { ... },
  "phoneNumber": "+441234567890",
  "email": "jane.smith@example.com"
}
```

**Responses:**

| Status | Description                              |
|--------|------------------------------------------|
| `200`  | Updated user details returned            |
| `400`  | Invalid request                          |
| `401`  | Missing or invalid token                 |
| `403`  | Not allowed to update this user          |
| `404`  | User not found                           |
| `500`  | Unexpected server error                  |

---

#### `DELETE /v1/users/{userId}`

Delete a user by their ID.

- **Auth required:** Yes
- **Path params:** `userId` — format `usr-[A-Za-z0-9]+`

> A user cannot be deleted while they are associated with a bank account.

**Responses:**

| Status | Description                                              |
|--------|----------------------------------------------------------|
| `204`  | User deleted successfully                                |
| `400`  | Invalid request                                          |
| `401`  | Missing or invalid token                                 |
| `403`  | Not allowed to delete this user                          |
| `404`  | User not found                                           |
| `409`  | User is still associated with a bank account             |
| `500`  | Unexpected server error                                  |

---

### Accounts

#### `POST /v1/accounts`

Create a new bank account for the authenticated user.

- **Auth required:** Yes

**Request body:**
```json
{
  "name": "Personal Bank Account",
  "accountType": "personal"
}
```

**Responses:**

| Status | Description                          |
|--------|--------------------------------------|
| `201`  | Account created successfully         |
| `400`  | Invalid request body                 |
| `401`  | Missing or invalid token             |
| `403`  | Not allowed to create an account     |
| `500`  | Unexpected server error              |

---

#### `GET /v1/accounts`

List all bank accounts belonging to the authenticated user.

- **Auth required:** Yes

**Responses:**

| Status | Description                          |
|--------|--------------------------------------|
| `200`  | List of accounts returned            |
| `401`  | Missing or invalid token             |
| `500`  | Unexpected server error              |

**Success response (`200`):**
```json
{
  "accounts": [
    {
      "accountNumber": "01234567",
      "sortCode": "10-10-10",
      "name": "Personal Bank Account",
      "accountType": "personal",
      "balance": 1000.00,
      "currency": "GBP",
      "createdTimestamp": "2026-01-01T10:00:00Z",
      "updatedTimestamp": "2026-01-10T12:00:00Z"
    }
  ]
}
```

---

#### `GET /v1/accounts/{accountNumber}`

Fetch a bank account by its account number.

- **Auth required:** Yes
- **Path params:** `accountNumber` — format `^01\d{6}$` (e.g. `01234567`)

**Responses:**

| Status | Description                                  |
|--------|----------------------------------------------|
| `200`  | Account details returned                     |
| `400`  | Invalid request                              |
| `401`  | Missing or invalid token                     |
| `403`  | Not allowed to access this account           |
| `404`  | Account not found                            |
| `500`  | Unexpected server error                      |

---

#### `PATCH /v1/accounts/{accountNumber}`

Update a bank account's details.

- **Auth required:** Yes
- **Path params:** `accountNumber` — format `^01\d{6}$`

**Request body** (all fields optional):
```json
{
  "name": "Updated Account Name",
  "accountType": "personal"
}
```

**Responses:**

| Status | Description                                  |
|--------|----------------------------------------------|
| `200`  | Updated account details returned             |
| `400`  | Invalid request                              |
| `401`  | Missing or invalid token                     |
| `403`  | Not allowed to update this account           |
| `404`  | Account not found                            |
| `500`  | Unexpected server error                      |

---

#### `DELETE /v1/accounts/{accountNumber}`

Delete a bank account by its account number.

- **Auth required:** Yes
- **Path params:** `accountNumber` — format `^01\d{6}$`

**Responses:**

| Status | Description                                  |
|--------|----------------------------------------------|
| `204`  | Account deleted successfully                 |
| `400`  | Invalid request                              |
| `401`  | Missing or invalid token                     |
| `403`  | Not allowed to delete this account           |
| `404`  | Account not found                            |
| `500`  | Unexpected server error                      |

---

### Transactions

#### `POST /v1/accounts/{accountNumber}/transactions`

Create a deposit or withdrawal transaction on an account.

- **Auth required:** Yes
- **Path params:** `accountNumber` — format `^01\d{6}$`

**Request body:**
```json
{
  "amount": 250.00,
  "currency": "GBP",
  "type": "deposit",
  "reference": "Optional reference"
}
```

| Field      | Values               |
|------------|----------------------|
| `type`     | `deposit`, `withdrawal` |
| `currency` | `GBP`               |
| `amount`   | `0.00` – `10000.00` |

**Responses:**

| Status | Description                                  |
|--------|----------------------------------------------|
| `201`  | Transaction created successfully             |
| `400`  | Invalid request body                         |
| `401`  | Missing or invalid token                     |
| `403`  | Not allowed to transact on this account      |
| `404`  | Account not found                            |
| `422`  | Insufficient funds for withdrawal            |
| `500`  | Unexpected server error                      |

---

#### `GET /v1/accounts/{accountNumber}/transactions`

List all transactions on a bank account.

- **Auth required:** Yes
- **Path params:** `accountNumber` — format `^01\d{6}$`

**Responses:**

| Status | Description                                  |
|--------|----------------------------------------------|
| `200`  | List of transactions returned                |
| `400`  | Invalid request                              |
| `401`  | Missing or invalid token                     |
| `403`  | Not allowed to view these transactions       |
| `404`  | Account not found                            |
| `500`  | Unexpected server error                      |

**Success response (`200`):**
```json
{
  "transactions": [
    {
      "id": "tan-abc123",
      "amount": 250.00,
      "currency": "GBP",
      "type": "deposit",
      "reference": "Optional reference",
      "createdTimestamp": "2026-01-15T09:30:00Z"
    }
  ]
}
```

---

#### `GET /v1/accounts/{accountNumber}/transactions/{transactionId}`

Fetch a single transaction by its ID.

- **Auth required:** Yes
- **Path params:**
  - `accountNumber` — format `^01\d{6}$`
  - `transactionId` — format `^tan-[A-Za-z0-9]+$`

**Responses:**

| Status | Description                                  |
|--------|----------------------------------------------|
| `200`  | Transaction details returned                 |
| `400`  | Invalid request                              |
| `401`  | Missing or invalid token                     |
| `403`  | Not allowed to view this transaction         |
| `404`  | Account or transaction not found             |
| `500`  | Unexpected server error                      |

---

## Error Responses

### Standard error

```json
{
  "message": "A description of what went wrong"
}
```

### Validation error (`400`)

```json
{
  "message": "Validation failed",
  "details": [
    {
      "field": "email",
      "message": "must be a valid email address",
      "type": "format"
    }
  ]
}
```

