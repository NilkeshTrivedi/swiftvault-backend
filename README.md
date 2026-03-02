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
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Phase Roadmap](#phase-roadmap)

---

## Overview

SwiftVault is a full-featured banking REST API that handles user registration, authentication, account management, financial transactions, fixed deposits, recurring deposits, loans, and virtual cards. Built with enterprise-grade practices including BCrypt password hashing, JWT stateless authentication, transactional integrity, and ownership verification on every operation.

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
| Scheduler | Spring Scheduler | FD maturity & RD auto-debit jobs |

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
│                             │  /api/fd/rates, /api/loans/rates → public
│                             │  /api/admin/** → ADMIN only
│                             │  everything else → authenticated
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     Controllers             │  Receive HTTP, delegate to services
│  AuthController             │  @AuthenticationPrincipal injects User
│  UserController             │
│  AccountController          │
│  AdminController            │
│  FixedDepositController     │
│  RecurringDepositController │
│  LoanController             │
│  CardController             │
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     Services                │  Business logic layer
│  UserServiceImpl            │  Validates rules, throws exceptions
│  AccountServiceImpl         │  @Transactional — auto rollback on error
│  FixedDepositServiceImpl    │
│  RecurringDepositServiceImpl│
│  LoanServiceImpl            │
│  CardServiceImpl            │
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     Repositories            │  Data access layer
│  UserRepository             │  Spring Data JPA — no manual SQL needed
│  AccountRepository          │
│  TransactionRepository      │
│  FixedDepositRepository     │
│  RecurringDepositRepository │
│  LoanRepository             │
│  VirtualCardRepository      │
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     MySQL Database          │
│  users                      │
│  accounts                   │
│  transactions               │
│  fixed_deposits             │
│  recurring_deposits         │
│  loans                      │
│  virtual_cards              │
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

### 🏧 Fixed Deposits (NEW)
- ✅ Open FD with minimum ₹1,000
- ✅ Tenure: 7 days to 10 years
- ✅ Tiered interest rates based on tenure (4% to 7.5% p.a.)
- ✅ Quarterly compounding interest calculation
- ✅ Payout options: ON_MATURITY / MONTHLY_INTEREST / QUARTERLY_INTEREST
- ✅ Nominee name support
- ✅ Premature withdrawal with 1% penalty on principal
- ✅ Auto-maturity processing via scheduled job (daily at midnight)
- ✅ View all FDs with maturity amount and interest earned
- ✅ Public interest rate info endpoint (no token required)

### 📅 Recurring Deposits (NEW)
- ✅ Open RD with minimum ₹500/month installment
- ✅ Tenure: 6 months to 10 years
- ✅ Fixed 6.5% p.a. interest rate
- ✅ Manual installment payment with PIN verification
- ✅ Auto-debit via scheduled job (daily at 9 AM)
- ✅ Missed installment tracking
- ✅ Premature closure (principal returned, no interest)
- ✅ Auto-maturity credit when all installments paid
- ✅ Nominee name support

### 🏠 Loans (NEW)
- ✅ Apply for PERSONAL / HOME / CAR / EDUCATION loans
- ✅ Competitive interest rates (8.5% to 12% p.a.)
- ✅ EMI calculator (public endpoint, no token required)
- ✅ Standard EMI formula: P × r × (1+r)^n / ((1+r)^n - 1)
- ✅ Admin approval/rejection workflow
- ✅ Loan disbursed directly to linked account on approval
- ✅ Monthly EMI payment with PIN verification
- ✅ Outstanding balance tracking
- ✅ Automatic loan closure when all EMIs paid
- ✅ One active loan per type per user
- ✅ Pending loan check before new application

### 💳 Virtual Cards (NEW)
- ✅ Issue DEBIT or CREDIT virtual cards
- ✅ Supports VISA / MASTERCARD / RUPAY networks
- ✅ Maximum 3 cards per user
- ✅ CVV generated and shown only once at issuance (BCrypt hashed in DB)
- ✅ 3-year expiry from issue date
- ✅ Freeze / unfreeze card
- ✅ Permanent block (irreversible)
- ✅ Customizable spending limits (daily / monthly / per-transaction)
- ✅ Feature toggles: online transactions, international payments, contactless
- ✅ Custom nickname for cards
- ✅ Spending tracker (today & month)

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
- ✅ View all pending loan applications
- ✅ Approve loans (disbursed instantly to account)
- ✅ Reject loans with reason

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

### Password & PIN Security
```
Algorithm  : BCrypt
Strength   : 12 rounds (~300ms per hash)
Storage    : Only the hash is stored, never plain text
PIN        : Separate 4-digit PIN, also BCrypt hashed
CVV        : Generated randomly, BCrypt hashed, shown only once
```

### Ownership Verification
```
Every single operation verifies:
  account.getUser().getUserId().equals(requestingUserId)
  card.getUser().getUserId().equals(requestingUserId)
  fd.getUser().getUserId().equals(requestingUserId)
  loan.getUser().getUserId().equals(requestingUserId)

If mismatch → 403 Forbidden immediately
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
| GET | `/api/fd/rates` | View FD interest rates |
| GET | `/api/rd/rates` | View RD information |
| GET | `/api/loans/rates` | View loan interest rates |
| GET | `/api/loans/calculate-emi` | Calculate EMI |

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
| GET | `/api/accounts/{number}/transactions?type=DEPOSIT` | Filtered history |
| GET | `/api/accounts/{number}/mini-statement` | Last 5 transactions |
| POST | `/api/fd/open` | Open Fixed Deposit |
| GET | `/api/fd` | Get all own FDs |
| GET | `/api/fd/{fdId}` | Get specific FD |
| POST | `/api/fd/withdraw` | Withdraw FD (premature or maturity) |
| POST | `/api/rd/open` | Open Recurring Deposit |
| GET | `/api/rd` | Get all own RDs |
| GET | `/api/rd/{rdId}` | Get specific RD |
| POST | `/api/rd/{rdId}/pay` | Pay RD installment |
| POST | `/api/rd/{rdId}/close` | Close RD prematurely |
| POST | `/api/loans/apply` | Apply for loan |
| GET | `/api/loans` | Get all own loans |
| GET | `/api/loans/{loanId}` | Get specific loan |
| POST | `/api/loans/pay-emi` | Pay loan EMI |
| POST | `/api/cards/issue` | Issue virtual card |
| GET | `/api/cards` | Get all own cards |
| GET | `/api/cards/{cardId}` | Get specific card |
| PUT | `/api/cards/{cardId}/freeze` | Freeze card |
| PUT | `/api/cards/{cardId}/unfreeze` | Unfreeze card |
| PUT | `/api/cards/{cardId}/block` | Permanently block card |
| PUT | `/api/cards/limits` | Update card limits |
| PUT | `/api/cards/{cardId}/toggle/{feature}` | Toggle card feature |

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
| GET | `/api/loans/admin/pending` | View pending loans |
| PUT | `/api/loans/admin/{loanId}/approve` | Approve loan |
| PUT | `/api/loans/admin/{loanId}/reject` | Reject loan |

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

TABLE fixed_deposits
  fd_id             VARCHAR(25)  PK   -- FD-XXXXXXXXXX
  user_id           VARCHAR(20)  FK → users
  source_account    VARCHAR(20)  FK → accounts
  principal_amount  DECIMAL(15,2)
  interest_rate     DECIMAL(5,2)
  tenure_days       INT
  maturity_amount   DECIMAL(15,2)
  start_date        DATE
  maturity_date     DATE
  status            ENUM(ACTIVE, MATURED, CLOSED, PREMATURE_CLOSED)
  payout_option     ENUM(ON_MATURITY, MONTHLY_INTEREST, QUARTERLY_INTEREST)
  nominee_name      VARCHAR(100)
  premature_withdrawal BOOLEAN
  actual_interest_earned DECIMAL(15,2)
  closed_at         DATETIME
  created_at        DATETIME

TABLE recurring_deposits
  rd_id               VARCHAR(25)  PK   -- RD-XXXXXXXXXX
  user_id             VARCHAR(20)  FK → users
  source_account      VARCHAR(20)  FK → accounts
  monthly_installment DECIMAL(15,2)
  tenure_months       INT
  interest_rate       DECIMAL(5,2)
  total_deposited     DECIMAL(15,2)
  maturity_amount     DECIMAL(15,2)
  installments_paid   INT
  installments_missed INT
  next_due_date       DATE
  start_date          DATE
  maturity_date       DATE
  status              ENUM(ACTIVE, MATURED, CLOSED, DEFAULTED)
  nominee_name        VARCHAR(100)
  closed_at           DATETIME
  created_at          DATETIME

TABLE loans
  loan_id           VARCHAR(25)  PK   -- LN-XXXXXXXXXX
  user_id           VARCHAR(20)  FK → users
  disbursal_account VARCHAR(20)  FK → accounts
  loan_type         ENUM(PERSONAL, HOME, CAR, EDUCATION)
  loan_amount       DECIMAL(15,2)
  interest_rate     DECIMAL(5,2)
  tenure_months     INT
  emi_amount        DECIMAL(15,2)
  total_payable     DECIMAL(15,2)
  total_interest    DECIMAL(15,2)
  outstanding_balance DECIMAL(15,2)
  emis_paid         INT
  emis_missed       INT
  next_emi_date     DATE
  disbursal_date    DATE
  purpose           VARCHAR(255)
  status            ENUM(PENDING, APPROVED, ACTIVE, CLOSED, REJECTED, DEFAULTED)
  rejection_reason  VARCHAR(500)
  admin_notes       VARCHAR(500)
  closed_at         DATETIME
  created_at        DATETIME

TABLE virtual_cards
  card_id                   VARCHAR(25)  PK   -- CRD-XXXXXXXXXX
  user_id                   VARCHAR(20)  FK → users
  linked_account            VARCHAR(20)  FK → accounts
  card_number               VARCHAR(16)  UNIQUE
  card_holder_name          VARCHAR(100)
  expiry_month              INT
  expiry_year               INT
  cvv_hash                  VARCHAR(255)      -- BCrypt hashed, shown once
  card_type                 ENUM(DEBIT, CREDIT)
  card_network              ENUM(VISA, MASTERCARD, RUPAY)
  status                    ENUM(ACTIVE, FROZEN, BLOCKED, EXPIRED)
  daily_limit               DECIMAL(15,2)
  monthly_limit             DECIMAL(15,2)
  per_txn_limit             DECIMAL(15,2)
  today_spent               DECIMAL(15,2)
  month_spent               DECIMAL(15,2)
  online_transactions       BOOLEAN
  international_transactions BOOLEAN
  contactless_payments      BOOLEAN
  nickname                  VARCHAR(50)
  issued_at                 DATETIME
  frozen_at                 DATETIME
  blocked_at                DATETIME
```

---

## FD Interest Rates

| Tenure | Interest Rate |
|---|---|
| 7 – 29 days | 4.00% p.a. |
| 30 – 90 days | 5.50% p.a. |
| 91 – 180 days | 6.00% p.a. |
| 181 – 365 days | 6.75% p.a. |
| 1 – 2 years | 7.00% p.a. |
| 2 – 3 years | 7.25% p.a. |
| 3+ years | 7.50% p.a. |
| Premature penalty | 1% on principal |
| Compounding | Quarterly |

---

## Loan Interest Rates

| Loan Type | Rate | Max Amount | Max Tenure |
|---|---|---|---|
| Personal | 12.00% p.a. | ₹5,00,000 | 60 months |
| Home | 8.50% p.a. | ₹1,00,00,000 | 360 months |
| Car | 9.50% p.a. | ₹20,00,000 | 84 months |
| Education | 10.00% p.a. | ₹20,00,000 | 120 months |

---

## Fraud Prevention

### What IS Prevented ✅

| Threat | Prevention Mechanism |
|---|---|
| **Unauthorized account access** | Ownership check on every operation |
| **Unauthorized FD/RD/Loan/Card access** | Ownership check on every operation |
| **Brute force login** | Account locked after 3 failed attempts for 30 minutes |
| **Password theft** | BCrypt strength 12 |
| **Token theft replay** | JWT expires in 24 hours |
| **Overdraft** | Balance check before every withdrawal and transfer |
| **Daily cash drain** | ₹50,000 daily withdrawal limit enforced per account |
| **Savings account depletion** | Minimum balance of ₹1,000 enforced |
| **Self-transfer abuse** | Cannot transfer from an account to itself |
| **Frozen account transactions** | FROZEN and CLOSED accounts reject all transactions |
| **Transaction tampering** | All transaction records are immutable |
| **CVV exposure** | CVV shown only once at card issuance, BCrypt hashed in DB |
| **Multiple active same-type loans** | One active loan per type per user enforced |
| **FD principal loss on premature exit** | 1% penalty clearly communicated and applied |
| **Mass data exposure** | DTOs never expose passwordHash, pinHash, or cvvHash |
| **CSRF attacks** | Stateless JWT — no session cookies |
| **SQL Injection** | Spring Data JPA parameterized queries |

### What CANNOT Be Done ❌

| Limitation | Reason | Future Fix |
|---|---|---|
| **Duplicate registration beyond email** | Only email uniqueness enforced | Add phone uniqueness constraint |
| **Real-time fraud pattern detection** | No ML model | Integrate fraud scoring service |
| **Velocity checks** | No transaction count rate limiting | Add rate limiting |
| **Device fingerprinting** | JWT doesn't track device | Store device info in token |
| **Concurrent transaction race conditions** | No row-level locking | Add `@Lock(PESSIMISTIC_WRITE)` |
| **Card payment processing** | Cards are virtual — no payment gateway | Integrate Razorpay / Stripe |
| **2FA / OTP** | Only password + PIN | Integrate Twilio SMS OTP |
| **Chargeback / dispute system** | Transactions are final | Add dispute workflow |
| **Loan credit score check** | No credit scoring | Integrate CIBIL API |
| **International transfer limits** | No distinction | Add transfer type classification |

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
  "message": "Insufficient balance. Available: 1500.00",
  "timestamp": "2026-03-01T13:49:31"
}


```

---

## Getting Started

### Prerequisites
- Java 22+
- Maven 3.9+
- MySQL 8.0+

### Setup

**1. Clone the repository**
```bash
git clone https://github.com/NilkeshTrivedi/swiftvault-backend.git
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

**5. Tables are auto-created by Hibernate on first run.**

**6. Create admin user in MySQL**
```sql
INSERT INTO users (user_id, full_name, email, phone, password_hash, role, status, failed_attempts, created_at)
VALUES (
  'USR-ADMIN001',
  'Admin User',
  'admin@swiftvault.com',
  '9000000000',
  '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
  'ADMIN', 'ACTIVE', 0, NOW()
);
-- Default password: "password"
```

**7. Test with Postman**
```
POST http://localhost:8080/api/auth/register
```

---

## Project Structure

```
src/main/java/com/swiftvault/backend/
├── controller/
│   ├── AuthController.java               # /api/auth/**
│   ├── UserController.java               # /api/users/**
│   ├── AccountController.java            # /api/accounts/**
│   ├── AdminController.java              # /api/admin/**
│   ├── FixedDepositController.java       # /api/fd/**
│   ├── RecurringDepositController.java   # /api/rd/**
│   ├── LoanController.java               # /api/loans/**
│   └── CardController.java              # /api/cards/**
├── service/
│   ├── UserService.java
│   ├── AccountService.java
│   ├── FixedDepositService.java
│   ├── RecurringDepositService.java
│   ├── LoanService.java
│   ├── CardService.java
│   └── impl/
│       ├── UserServiceImpl.java
│       ├── AccountServiceImpl.java
│       ├── FixedDepositServiceImpl.java
│       ├── RecurringDepositServiceImpl.java
│       ├── LoanServiceImpl.java
│       └── CardServiceImpl.java
├── entity/
│   ├── User.java
│   ├── Account.java
│   ├── Transaction.java
│   ├── FixedDeposit.java
│   ├── RecurringDeposit.java
│   ├── Loan.java
│   └── VirtualCard.java
├── repository/
│   ├── UserRepository.java
│   ├── AccountRepository.java
│   ├── TransactionRepository.java
│   ├── FixedDepositRepository.java
│   ├── RecurringDepositRepository.java
│   ├── LoanRepository.java
│   └── VirtualCardRepository.java
├── dto/
│   ├── request/                          # Incoming API payloads
│   └── response/                         # Outgoing API responses
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthFilter.java
│   ├── SecurityConfig.java
│   └── CorsConfig.java
├── exception/
│   ├── SwiftVaultException.java
│   └── GlobalExceptionHandler.java
└── util/
    ├── IdGenerator.java
    └── ScheduledJobs.java                # FD maturity & RD auto-debit
```

---

## Scheduled Jobs

| Job | Schedule | Purpose |
|---|---|---|
| `processMaturedFds()` | Daily at 12:00 AM | Auto-credits matured FDs to source account |
| `processAutoDebitRds()` | Daily at 9:00 AM | Auto-debits RD installments from source account |

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
             └── Full REST API (55+ endpoints)
             └── Fixed Deposits with tiered rates
             └── Recurring Deposits with auto-debit
             └── Loans with EMI calculator
             └── Virtual Cards with controls
             └── Scheduled background jobs
             └── Ownership verification on all resources
             └── Transaction integrity

Phase 3 🔜  React Frontend
             └── Login / Register pages
             └── Dashboard with account summary
             └── Deposit / Withdraw / Transfer UI
             └── FD / RD / Loan management UI
             └── Virtual card management
             └── Transaction history with filters
             └── Admin panel

Phase 4 🔜  Production Hardening
             └── Docker containerization
             └── CI/CD pipeline
             └── Rate limiting
             └── 2FA (OTP via SMS)
             └── Deployed to AWS / Railway
```

---

<div align="center">
Built by Nilkesh Trivedi | SwiftVault Banking System
</div>