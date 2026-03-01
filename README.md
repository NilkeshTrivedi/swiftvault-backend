# 🏦 SwiftVault Banking Backend

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen)
![Java](https://img.shields.io/badge/Java-22-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![JWT](https://img.shields.io/badge/JWT-0.12.6-purple)
![License](https://img.shields.io/badge/License-MIT-yellow)

**A production-grade RESTful banking backend built with Spring Boot, MySQL, and JWT authentication.**

</div>

---

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [Security](#security)
- [API Endpoints](#api-endpoints)
- [Database Schema](#database-schema)
- [Fraud Prevention](#fraud-prevention)
- [What Cannot Be Done](#what-cannot-be-done)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Phase Roadmap](#phase-roadmap)

---

## Overview

SwiftVault is a full-featured banking REST API that handles user registration, authentication, account management, and financial transactions. Built with enterprise-grade practices including BCrypt password hashing, JWT stateless authentication, transactional integrity, and ownership verification on every operation.

This is **Phase 2** of a 3-phase project:
- ✅ **Phase 1** — Java Console Application (file-based persistence)
- ✅ **Phase 2** — Spring Boot REST API (MySQL + JWT)
- 🔜 **Phase 3** — React Frontend

---

## Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| Language | Java 22 | Core language |
| Framework | Spring Boot 4.0.3 | Application framework |
| Security | Spring Security 7 + JWT | Authentication & authorization |
| Database | MySQL 8.0 | Persistent storage |
| ORM | Hibernate 7 / JPA | Object-relational mapping |
| Password Hashing | BCrypt (strength 12) | Secure password storage |
| Token | JJWT 0.12.6 | JWT generation & validation |
| Validation | Jakarta Validation | Request input validation |
| Build Tool | Maven | Dependency management |
| Server | Apache Tomcat 11 | Embedded web server |

---

## Architecture

```
Client (Postman / React)
         │
         ▼
  HTTP Request
         │
         ▼
┌─────────────────────────────┐
│     JwtAuthFilter           │  Validates Bearer token on every request
│     (OncePerRequestFilter)  │  Sets authenticated user in SecurityContext
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     SecurityFilterChain     │  Checks URL-level permissions
│     (Spring Security)       │  /api/auth/** → public
│                             │  /api/admin/** → ADMIN only
│                             │  everything else → authenticated
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     Controllers             │  Receive HTTP, delegate to services
│  AuthController             │  @AuthenticationPrincipal injects user
│  UserController             │
│  AccountController          │
│  AdminController            │
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     Services                │  Business logic layer
│  UserServiceImpl            │  Validates rules, throws exceptions
│  AccountServiceImpl         │  @Transactional — auto rollback on error
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     Repositories            │  Data access layer
│  UserRepository             │  Spring Data JPA — no manual SQL needed
│  AccountRepository          │
│  TransactionRepository      │
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     MySQL Database          │
│  users                      │
│  accounts                   │
│  transactions               │
└─────────────────────────────┘
```

---

## Features

### 👤 User Management
- ✅ Register with full name, email, phone, password
- ✅ Login with email + password
- ✅ JWT token returned on login/register (24hr expiry)
- ✅ View own profile
- ✅ Change password (requires current password verification)
- ✅ Set 4-digit transaction PIN (BCrypt hashed)
- ✅ Login lockout after 3 failed attempts (30 minute lock)
- ✅ Auto-unlock after lockout period expires

### 🏦 Account Management
- ✅ Open SAVINGS or CHECKING account
- ✅ Multiple accounts per user
- ✅ Set custom nickname for accounts
- ✅ View all own accounts with balances
- ✅ SAVINGS minimum balance: ₹1,000
- ✅ Daily withdrawal limit: ₹50,000
- ✅ View remaining daily withdrawal limit

### 💸 Transactions
- ✅ Deposit money into own account
- ✅ Withdraw money (requires transaction PIN)
- ✅ Transfer to another account by account number
- ✅ Transfer to another user by their email
- ✅ Full transaction history
- ✅ Mini statement (last 5 transactions)
- ✅ Filter transactions by type (DEPOSIT / WITHDRAW / TRANSFER)
- ✅ All transactions are immutable (audit trail)

### 👨‍💼 Admin Operations
- ✅ View dashboard stats (users, accounts, balances, transactions)
- ✅ View all users
- ✅ Search users by name, email, or ID
- ✅ Suspend / activate user accounts
- ✅ Reset any user's password
- ✅ View all accounts
- ✅ Freeze / unfreeze accounts
- ✅ Close accounts (only if balance is zero)
- ✅ Apply monthly interest to all SAVINGS accounts (4% p.a.)
- ✅ Apply low balance fees (₹100 if below minimum)
- ✅ Export all transactions to CSV

---

## Security

### Authentication Flow
```
1. User sends email + password to POST /api/auth/login
2. Server verifies BCrypt hash
3. Server generates JWT signed with HS384 algorithm
4. JWT contains: userId, email, role, issuedAt, expiration
5. Client stores token and sends it as:
      Authorization: Bearer eyJhbGci...
6. JwtAuthFilter intercepts every request and validates token
7. Valid token → user loaded from DB → set in SecurityContext
8. Controller receives @AuthenticationPrincipal User user automatically
```

### Password Security
```
Algorithm  : BCrypt
Strength   : 12 rounds (~300ms per hash — fast enough for UX, slow enough to prevent brute force)
Storage    : Only the hash is stored, never plain text
Recovery   : No password recovery (reset only via admin or change-password endpoint)
```

### Transaction PIN Security
```
- Separate 4-digit PIN required for all money movements (withdraw, transfer)
- Also BCrypt hashed in database
- Independent from login password
- Cannot perform transactions without setting PIN first
```

### Ownership Verification
```
Every single account operation verifies:
  account.getUser().getUserId().equals(requestingUserId)

If a user tries to access or transact on someone else's account:
  → 403 Forbidden immediately
  → Warning logged with userId and attempted action
```

### JWT Configuration
```
Algorithm  : HS384
Expiry     : 24 hours (86400000ms)
Secret     : Configurable via application.properties
Claims     : sub (userId), email, role, iat, exp
```

---

## API Endpoints

### Public (No token required)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT token |

### Customer (Token required)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me` | Get own profile |
| PUT | `/api/users/me/password` | Change password |
| POST | `/api/users/me/pin` | Set transaction PIN |
| GET | `/api/accounts` | Get all own accounts |
| POST | `/api/accounts` | Open new account |
| GET | `/api/accounts/{number}` | Get specific account |
| PUT | `/api/accounts/{number}/nickname` | Set account nickname |
| POST | `/api/accounts/deposit` | Deposit money |
| POST | `/api/accounts/withdraw` | Withdraw money |
| POST | `/api/accounts/transfer` | Transfer money |
| GET | `/api/accounts/{number}/transactions` | Full transaction history |
| GET | `/api/accounts/{number}/mini-statement` | Last 5 transactions |

### Admin (Token + ADMIN role required)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/dashboard` | Bank statistics |
| GET | `/api/admin/users` | All users |
| GET | `/api/admin/users/search?q=` | Search users |
| PUT | `/api/admin/users/{id}/suspend` | Suspend user |
| PUT | `/api/admin/users/{id}/activate` | Activate user |
| PUT | `/api/admin/users/{id}/reset-password` | Reset password |
| GET | `/api/admin/accounts` | All accounts |
| PUT | `/api/admin/accounts/{number}/freeze` | Freeze account |
| PUT | `/api/admin/accounts/{number}/unfreeze` | Unfreeze account |
| PUT | `/api/admin/accounts/{number}/close` | Close account |
| POST | `/api/admin/operations/apply-interest` | Apply monthly interest |
| POST | `/api/admin/operations/apply-fees` | Apply low balance fees |
| POST | `/api/admin/operations/export-csv` | Export transactions CSV |

---

## Database Schema

```sql
TABLE users
  user_id         VARCHAR(20)  PK   -- USR-XXXXXXXX
  full_name       VARCHAR(100) NN
  email           VARCHAR(100) UNIQUE NN
  phone           VARCHAR(15)
  password_hash   VARCHAR(255) NN   -- BCrypt $2a$12$...
  pin_hash        VARCHAR(255)      -- BCrypt $2a$12$...
  role            ENUM(CUSTOMER, ADMIN)
  status          ENUM(ACTIVE, SUSPENDED, LOCKED)
  failed_attempts INT
  locked_until    DATETIME
  last_login      DATETIME
  created_at      DATETIME

TABLE accounts
  account_number    VARCHAR(20)  PK   -- ACC-XXXXXXXX
  user_id           VARCHAR(20)  FK → users
  balance           DECIMAL(15,2)
  type              ENUM(SAVINGS, CHECKING)
  status            ENUM(ACTIVE, FROZEN, CLOSED)
  nickname          VARCHAR(50)
  today_withdrawn   DECIMAL(15,2)
  withdraw_date     DATE
  last_interest_applied DATETIME
  created_at        DATETIME

TABLE transactions
  transaction_id  VARCHAR(30)  PK   -- TXN-XXXXXXXXXXXX
  from_account    VARCHAR(20)  NN   (indexed)
  to_account      VARCHAR(20)
  type            ENUM(DEPOSIT, WITHDRAW, TRANSFER)
  amount          DECIMAL(15,2)
  description     VARCHAR(255)
  timestamp       DATETIME         (indexed)
  -- ALL COLUMNS updatable=false (immutable audit trail)
```

---

## Fraud Prevention

### What IS Prevented ✅

| Threat | Prevention Mechanism |
|---|---|
| **Unauthorized account access** | Ownership check on every operation — userId must match account owner |
| **Brute force login** | Account locked after 3 failed attempts for 30 minutes |
| **Password theft** | BCrypt strength 12 — even if DB is stolen, passwords cannot be reversed |
| **Token theft replay** | JWT expires in 24 hours — stolen tokens become invalid |
| **Overdraft** | Balance check before every withdrawal and transfer |
| **Daily cash drain** | ₹50,000 daily withdrawal limit enforced per account |
| **Savings account depletion** | Minimum balance of ₹1,000 enforced on all SAVINGS withdrawals |
| **Self-transfer abuse** | Cannot transfer from an account to itself |
| **Frozen account transactions** | FROZEN and CLOSED accounts reject all transactions |
| **Transaction tampering** | All transaction records are immutable (`updatable=false`) |
| **Audit trail deletion** | Transactions are never deleted, only inserted |
| **Mass data exposure** | DTOs never expose passwordHash or pinHash to API responses |
| **CSRF attacks** | Stateless JWT — no session cookies, CSRF not applicable |
| **SQL Injection** | Spring Data JPA uses parameterized queries — no raw SQL |

### What CANNOT Be Done ❌

| Limitation | Reason | Future Fix |
|---|---|---|
| **Duplicate registration detection beyond email** | Only email uniqueness enforced — same person could register with different email | Add phone number uniqueness constraint |
| **Real-time fraud pattern detection** | No ML model or rule engine analyzing transaction patterns | Integrate a fraud scoring service |
| **Velocity checks** | No limit on number of transactions per hour | Add transaction count rate limiting |
| **Device fingerprinting** | JWT doesn't track which device logged in | Store device info in token or DB |
| **Concurrent transaction race conditions** | Two simultaneous withdrawals could theoretically both pass balance check | Add database-level row locking (`@Lock(PESSIMISTIC_WRITE)`) |
| **International transfer limits** | No distinction between domestic and international | Add transfer type classification |
| **Suspicious IP detection** | No IP tracking or geo-blocking | Add request IP logging and anomaly detection |
| **Account takeover via email change** | No email change endpoint yet | When added, require re-verification |
| **Chargeback / dispute system** | Transactions are final — no reversal mechanism | Add transaction dispute workflow |
| **Two-factor authentication (2FA)** | Only password + PIN, no OTP via SMS/email | Integrate Twilio or similar |
| **Card-not-present fraud** | No card system exists yet | Phase 4 feature |

---

## Getting Started

### Prerequisites
- Java 22+
- Maven 3.9+
- MySQL 8.0+

### Setup

**1. Clone the repository**
```bash
git clone https://github.com/yourusername/swiftvault-backend.git
cd swiftvault-backend
```

**2. Create MySQL database**
```sql
CREATE DATABASE swiftvault;
```

**3. Configure application.properties**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/swiftvault
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

jwt.secret=your-secret-key-minimum-32-characters
jwt.expiration=86400000
```

**4. Run**
```bash
mvn spring-boot:run
```

**5. Tables are created automatically by Hibernate on first run.**

**6. Test with Postman**
```
POST http://localhost:8080/api/auth/register
```

---

## Project Structure

```
src/main/java/com/swiftvault/backend/
├── controller/
│   ├── AuthController.java       # /api/auth/**
│   ├── UserController.java       # /api/users/**
│   ├── AccountController.java    # /api/accounts/**
│   └── AdminController.java      # /api/admin/**
├── service/
│   ├── UserService.java          # Interface
│   ├── AccountService.java       # Interface
│   └── impl/
│       ├── UserServiceImpl.java  # Business logic
│       └── AccountServiceImpl.java
├── entity/
│   ├── User.java                 # users table
│   ├── Account.java              # accounts table
│   └── Transaction.java         # transactions table
├── repository/
│   ├── UserRepository.java
│   ├── AccountRepository.java
│   └── TransactionRepository.java
├── dto/
│   ├── request/                  # Incoming API payloads
│   └── response/                 # Outgoing API responses
├── security/
│   ├── JwtUtil.java              # Token generation & validation
│   ├── JwtAuthFilter.java        # Intercepts every request
│   └── SecurityConfig.java      # Security rules
├── exception/
│   ├── SwiftVaultException.java  # Custom business exceptions
│   └── GlobalExceptionHandler.java # Converts exceptions to JSON
└── util/
    └── IdGenerator.java          # USR-/ACC-/TXN- ID generation
```

---

## Phase Roadmap

```
Phase 1 ✅  Java Console App
             └── File-based persistence (.dat files)
             └── Role-based menus (CUSTOMER / ADMIN)
             └── Full banking operations

Phase 2 ✅  Spring Boot REST API  ← YOU ARE HERE
             └── MySQL database
             └── JWT authentication
             └── BCrypt password hashing
             └── Full REST API (34 endpoints)
             └── Ownership verification
             └── Transaction integrity

Phase 3 🔜  React Frontend
             └── Login / Register pages
             └── Dashboard with account summary
             └── Deposit / Withdraw / Transfer UI
             └── Transaction history with filters
             └── Admin panel

Phase 4 🔜  Production Hardening
             └── Docker containerization
             └── CI/CD pipeline
             └── Rate limiting
             └── 2FA (OTP)
             └── Deployed to AWS / Railway
```

---

## Standard API Response Format

Every endpoint returns the same structure:

```json
{
  "success": true,
  "message": "Operation description",
  "data": { ... },
  "timestamp": "2026-03-01T13:49:31"
}
```

Error responses:
```json
{
  "success": false,
  "message": "Email already registered: test@gmail.com",
  "timestamp": "2026-03-01T13:49:31"
}
```

---

<div align="center">
Built by Nilkesh Trivedi | SwiftVault Banking System
</div>