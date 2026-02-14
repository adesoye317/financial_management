# 💰 Xpenskey — Financial Management Platform

A comprehensive financial management REST API built with **Spring Boot 3.5** and **Java 21**. Xpenskey empowers users to manage wallets, track savings goals, invest in pools, buy & sell on a marketplace, and gain AI-driven financial insights.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [API Endpoints](#-api-endpoints)
- [Database Schema](#-database-schema)
- [Security](#-security)
- [Scheduled Jobs](#-scheduled-jobs)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

### 🔐 Authentication & Security
- Email/password registration with OTP verification
- JWT-based stateless authentication with refresh tokens
- Account lockout after 5 failed login attempts (30-min cooldown)
- Email enumeration prevention
- SecureRandom OTP generation with SHA-256 hashing
- Rate limiting (30 requests/min per user)

### 💳 Wallet Management
- Auto-generated 10-digit account numbers
- Fund wallet, transfer to other users, withdraw to bank
- Pessimistic locking prevents double-spend race conditions
- Deadlock-safe transfers with consistent lock ordering
- Full transaction history with pagination

### 🏦 Bank Account Management
- Link up to 5 bank accounts per user
- 10-digit Nigerian account number validation
- Activate, deactivate, and manage linked accounts
- Account number masking in audit logs

### 🎯 Financial Goals
- Create savings goals with target amounts and deadlines
- Track progress with auto-calculated percentages
- Auto-achieve goals when target is met
- Fund and withdraw from goals
- Categories: House, Emergency, Education, Vacation, Retirement, and more

### 📈 Investment Module
- Browse investment pools filtered by risk level (LOW/MEDIUM/HIGH)
- Invest directly from wallet with automatic debit
- Daily ROI calculation via scheduled jobs
- Early withdrawal with configurable penalty
- Portfolio tracking with returns and performance metrics
- Admin pool creation and management

### 🛒 Marketplace
- List items for sale with categories and pricing
- Search and filter listings
- Wallet-based escrow payments
- Order lifecycle: Pending → Confirmed → Shipped → Delivered
- Automatic fund release to seller on delivery confirmation

### 📊 Financial Insights
- Dashboard with income, expenses, savings rate, and month-over-month changes
- Spending analysis with category breakdown and monthly trends
- Income analysis by source
- Goal progress tracking (on-track vs behind-schedule detection)
- Investment performance analytics
- Personalized recommendations engine
- Cash flow forecasting with confidence scores

### 📋 Audit & Compliance
- Every action logged asynchronously to `audit_logs`
- 25+ audit event types covering all modules
- User ID, action, details, IP address, and timestamp recorded
- DB-configurable transaction limits (min/max/daily)

---

## 🏗 Architecture

```
┌──────────────┐     ┌──────────────────────────┐     ┌──────────────┐
│   Frontend   │────▶│      Spring Boot          │────▶│  PostgreSQL  │
│  (Web/Mobile)│◀────│      REST API             │◀────│   Database   │
└──────────────┘     └──────┬───────────┬────────┘     └──────────────┘
                            │           │
                    ┌───────┼───────┐   │
                    ▼       ▼       ▼   ▼
              ┌─────────┐ ┌─────┐ ┌──────────┐ ┌───────────────┐
              │  SMTP   │ │ JWT │ │  Audit   │ │  Scheduler    │
              │ (Email) │ │Auth │ │  Logger  │ │ (Investment)  │
              └─────────┘ └─────┘ └──────────┘ └───────────────┘
```

### Module Interactions

```
Auth ──▶ Wallet (auto-create on signup)
Wallet ──▶ Investment (debit to invest)
Investment ──▶ Wallet (credit returns/withdrawals)
Wallet ──▶ Goals (fund goals from wallet)
Wallet ──▶ Marketplace (escrow payments)
Marketplace ──▶ Wallet (release to seller on delivery)
Wallet ──▶ Bank (withdrawals to bank)
Insights ◀── All Modules (reads transaction data)
Admin ──▶ All Modules (limits, pools, moderation)
```

---

## 🛠 Tech Stack

| Component      | Technology                          |
|----------------|-------------------------------------|
| Runtime        | Java 21                             |
| Framework      | Spring Boot 3.5                     |
| Database       | PostgreSQL 16                       |
| ORM            | Hibernate / Spring Data JPA         |
| Security       | Spring Security + JWT (JJWT 0.11.5) |
| Email          | Spring Mail + Gmail SMTP            |
| Scheduling     | Spring @Scheduled                   |
| Caching        | Spring Cache (in-memory)            |
| Build          | Maven                               |
| Monitoring     | Spring Actuator                     |

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 14+

### 1. Clone the repository

```bash
git clone https://github.com/your-org/xpenskey.git
cd xpenskey
```

### 2. Create the database

```bash
psql -U your_username
```

```sql
CREATE DATABASE xpenskey;
CREATE USER xpenskey_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE xpenskey TO xpenskey_user;
```

### 3. Set environment variables

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/xpenskey
export DB_USERNAME=xpenskey_user
export DB_PASSWORD=your_secure_password
export JWT_SECRET_KEY=your-256-bit-secret-key-minimum-32-characters
export MAIL_PASSWORD=your_gmail_app_password
```

### 4. Build and run

```bash
mvn clean install
mvn spring-boot:run
```

The API will be available at `http://localhost:8090/xpenskey`

### 5. Verify

```bash
curl http://localhost:8090/xpenskey/actuator/health
```

---

## ⚙ Configuration

### application.yml

```yaml
server:
  port: 8090
  servlet:
    context-path: /xpenskey

spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      poolName: XPENSKEY
      maximumPoolSize: 5
      minimumIdle: 5

  jpa:
    show-sql: false
    hibernate:
      ddl-auto: update    # Use 'validate' in production with Flyway

  mail:
    host: smtp.gmail.com
    port: 587
    username: support@xpenskey.com
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        debug: false
        smtp:
          auth: true
          starttls:
            enable: true

secret:
  key: ${JWT_SECRET_KEY}

logging:
  level:
    com.sun.mail: OFF
    jakarta.mail: OFF
    org.eclipse.angus.mail: OFF
    org.springframework.mail: OFF
```

### Transaction Limits (Database-Configurable)

| Limit Key                | Default Value | Description              |
|--------------------------|---------------|--------------------------|
| MIN_TRANSACTION_AMOUNT   | ₦1.00         | Minimum per transaction  |
| MAX_SINGLE_TRANSACTION   | ₦5,000,000    | Maximum per transaction  |
| DAILY_TRANSACTION_LIMIT  | ₦10,000,000   | Maximum per day          |

Limits are seeded via `data.sql` and updatable at runtime via admin API.

---

## 📡 API Endpoints

### Authentication

| Method | Endpoint                     | Description                    | Auth |
|--------|------------------------------|--------------------------------|------|
| POST   | `/api/auth/signup`           | Register new user              | ❌   |
| POST   | `/api/auth/login`            | Login and get JWT              | ❌   |
| POST   | `/api/auth/verify-otp`       | Verify email with OTP          | ✅   |
| POST   | `/api/auth/resend-otp`       | Resend OTP                     | ❌   |
| POST   | `/api/auth/refresh-token`    | Refresh JWT token              | ✅   |
| POST   | `/api/auth/setup-business`   | Complete profile setup         | ✅   |
| GET    | `/api/auth/get-user`         | Get current user profile       | ✅   |

### Wallet

| Method | Endpoint                     | Description                    | Auth |
|--------|------------------------------|--------------------------------|------|
| POST   | `/api/v1/wallet/create`      | Create wallet                  | ✅   |
| GET    | `/api/v1/wallet`             | Get wallet details             | ✅   |
| POST   | `/api/v1/wallet/fund`        | Fund wallet                    | ✅   |
| POST   | `/api/v1/wallet/transfer`    | Transfer to another wallet     | ✅   |
| POST   | `/api/v1/wallet/withdraw`    | Withdraw to bank               | ✅   |
| GET    | `/api/v1/wallet/transactions`| Get transaction history        | ✅   |

### Bank Accounts

| Method | Endpoint                                   | Description              | Auth |
|--------|--------------------------------------------|--------------------------|------|
| POST   | `/api/v1/bank-accounts`                    | Add bank account         | ✅   |
| GET    | `/api/v1/bank-accounts`                    | List all accounts        | ✅   |
| GET    | `/api/v1/bank-accounts/{bankId}`           | Get single account       | ✅   |
| POST   | `/api/v1/bank-accounts/update/bank-details`| Update account           | ✅   |
| POST   | `/api/v1/bank-accounts/deactivate`         | Deactivate account       | ✅   |
| DELETE | `/api/v1/bank-accounts/{bankId}`           | Delete account           | ✅   |

### Financial Goals

| Method | Endpoint                           | Description              | Auth |
|--------|------------------------------------|--------------------------|------|
| POST   | `/api/v1/goals`                    | Create goal              | ✅   |
| GET    | `/api/v1/goals/summary`            | Get goals summary        | ✅   |
| GET    | `/api/v1/goals/{goalId}`           | Get single goal          | ✅   |
| PUT    | `/api/v1/goals/{goalId}`           | Update goal              | ✅   |
| POST   | `/api/v1/goals/{goalId}/fund`      | Add funds to goal        | ✅   |
| POST   | `/api/v1/goals/{goalId}/withdraw`  | Withdraw from goal       | ✅   |
| DELETE | `/api/v1/goals/{goalId}`           | Cancel goal              | ✅   |

### Investments

| Method | Endpoint                                     | Description              | Auth |
|--------|----------------------------------------------|--------------------------|------|
| GET    | `/api/v1/investments/pools`                  | Browse pools             | ✅   |
| GET    | `/api/v1/investments/pools/{poolId}`         | Get pool details         | ✅   |
| POST   | `/api/v1/investments/invest`                 | Invest in pool           | ✅   |
| GET    | `/api/v1/investments/portfolio`              | Get portfolio            | ✅   |
| GET    | `/api/v1/investments/{investmentRef}`        | Get single investment    | ✅   |
| POST   | `/api/v1/investments/{investmentRef}/withdraw`| Withdraw investment     | ✅   |
| GET    | `/api/v1/investments/transactions`           | Investment tx history    | ✅   |

### Marketplace

| Method | Endpoint                                     | Description              | Auth |
|--------|----------------------------------------------|--------------------------|------|
| POST   | `/api/v1/marketplace/listings`               | Create listing           | ✅   |
| GET    | `/api/v1/marketplace/listings`               | Browse listings          | ✅   |
| GET    | `/api/v1/marketplace/listings/{listingId}`   | Get listing details      | ✅   |
| GET    | `/api/v1/marketplace/my-listings`            | Get my listings          | ✅   |
| PUT    | `/api/v1/marketplace/listings/{listingId}`   | Update listing           | ✅   |
| DELETE | `/api/v1/marketplace/listings/{listingId}`   | Remove listing           | ✅   |
| POST   | `/api/v1/marketplace/orders`                 | Place order              | ✅   |
| GET    | `/api/v1/marketplace/orders`                 | Get my orders            | ✅   |
| GET    | `/api/v1/marketplace/orders/sales`           | Get my sales             | ✅   |
| GET    | `/api/v1/marketplace/orders/{orderId}`       | Get order details        | ✅   |
| PATCH  | `/api/v1/marketplace/orders/{orderId}/status`| Update order status      | ✅   |
| POST   | `/api/v1/marketplace/orders/{orderId}/cancel`| Cancel order             | ✅   |

### Insights

| Method | Endpoint                             | Description              | Auth |
|--------|--------------------------------------|--------------------------|------|
| GET    | `/api/v1/insights/dashboard`         | Financial dashboard      | ✅   |
| GET    | `/api/v1/insights/spending`          | Spending analysis        | ✅   |
| GET    | `/api/v1/insights/income`            | Income analysis          | ✅   |
| GET    | `/api/v1/insights/investments`       | Investment insights      | ✅   |
| GET    | `/api/v1/insights/goals`             | Goal insights            | ✅   |
| GET    | `/api/v1/insights/recommendations`   | Personalized tips        | ✅   |
| GET    | `/api/v1/insights/cashflow`          | Cash flow forecast       | ✅   |

### Admin

| Method | Endpoint                                        | Description            | Auth  |
|--------|-------------------------------------------------|------------------------|-------|
| GET    | `/api/v1/admin/limits`                          | Get all limits         | ADMIN |
| PUT    | `/api/v1/admin/limits/{limitKey}`               | Update limit           | ADMIN |
| POST   | `/api/v1/admin/investments/pools`               | Create investment pool | ADMIN |
| PATCH  | `/api/v1/admin/investments/pools/{poolId}/status`| Update pool status    | ADMIN |

---

## 🗄 Database Schema

### Tables (13)

| Table                    | Description                  |
|--------------------------|------------------------------|
| `users`                  | User accounts & profiles     |
| `wallets`                | User wallet balances         |
| `wallet_transactions`    | Wallet transaction history   |
| `bank_account`           | Linked bank accounts         |
| `financial_goals`        | Savings goals                |
| `goal_transactions`      | Goal funding/withdrawal tx   |
| `investment_pools`       | Available investment pools   |
| `user_investments`       | User's active investments    |
| `investment_transactions`| Investment transaction log   |
| `marketplace_listings`   | Items listed for sale        |
| `marketplace_orders`     | Purchase orders              |
| `transaction_limits`     | Configurable tx limits       |
| `audit_logs`             | Full audit trail             |

### Key Relationships

```
users          1:1  wallets
users          1:N  bank_account (max 5)
wallets        1:N  wallet_transactions
users          1:N  financial_goals
financial_goals 1:N goal_transactions
investment_pools 1:N user_investments
users          1:N  user_investments
user_investments 1:N investment_transactions
users          1:N  marketplace_listings (as seller)
marketplace_listings 1:N marketplace_orders
```

---

## 🔒 Security

### Authentication Flow

```
Signup → Send OTP → Verify OTP → Login → JWT Issued → Access API → Refresh Token
```

### Security Features

| Feature                  | Details                                           |
|--------------------------|---------------------------------------------------|
| Password Hashing         | BCrypt                                            |
| OTP Security             | SecureRandom generation, SHA-256 hashed storage   |
| Token Authentication     | JWT HS256, configurable expiry                    |
| Account Protection       | 5 failed attempts → 30-min lockout                |
| Email Enumeration        | Generic responses prevent email discovery          |
| OTP Rate Limiting        | 1 minute cooldown between resends                 |
| API Rate Limiting        | 30 requests/min per user                          |
| Transaction Safety       | Pessimistic locking (SELECT FOR UPDATE)           |
| Deadlock Prevention      | Consistent lock ordering for transfers            |
| Atomic Transactions      | @Transactional on all mutations                   |
| Data Protection          | @JsonIgnore on passwords, OTPs                    |
| Audit Trail              | All actions logged asynchronously                 |
| Security Headers         | CSP, X-Frame-Options: DENY, stateless sessions    |

---

## ⏰ Scheduled Jobs

| Job                      | Schedule            | Description                                    |
|--------------------------|---------------------|------------------------------------------------|
| Daily ROI Calculation    | Every day at 00:00  | Calculates daily returns for active investments|
| Monthly Return Recording | 1st of every month  | Records monthly return transactions            |

---

## 📁 Project Structure

```
com.financal.mgt.Financal.Management
├── controller/
│   ├── AuthController
│   ├── BankAccountController
│   ├── WalletController
│   ├── FinancialGoalController
│   ├── InvestmentController
│   ├── AdminInvestmentController
│   ├── MarketplaceController
│   ├── InsightController
│   └── AdminController
├── service/
│   ├── AuthServiceImpl
│   ├── BankAccountServiceImpl
│   ├── WalletServiceImpl
│   ├── FinancialGoalServiceImpl
│   ├── InvestmentServiceImpl
│   ├── MarketplaceServiceImpl
│   ├── InsightServiceImpl
│   ├── TransactionLimitService
│   └── AuditService
├── model/
│   ├── User
│   ├── BankAccount
│   ├── wallet/ (Wallet, WalletTransaction)
│   ├── goal/ (FinancialGoal, GoalTransaction)
│   ├── investment/ (InvestmentPool, UserInvestment, InvestmentTransaction)
│   └── marketplace/ (MarketplaceListing, MarketplaceOrder)
├── repository/
│   ├── UserRepository
│   ├── account/ (BankAccountRepository)
│   ├── wallet/ (WalletRepository, WalletTransactionRepository)
│   ├── goal/ (FinancialGoalRepository, GoalTransactionRepository)
│   └── investment/ (InvestmentPoolRepository, UserInvestmentRepository, ...)
├── dto/
│   ├── request/ (per module)
│   └── response/ (per module)
├── enums/
│   ├── wallet/ (TransactionType, TransactionStatus)
│   ├── goal/ (GoalStatus, GoalCategory)
│   └── investment/ (RiskLevel, PoolStatus, InvestmentStatus, ...)
├── exception/
│   ├── GlobalExceptionHandler
│   ├── WalletNotFoundException
│   ├── InsufficientBalanceException
│   ├── InvalidTransactionException
│   └── DuplicateWalletException
├── security/
│   ├── SecurityConfig
│   ├── JwtUtil
│   ├── JwtAuthFilter
│   └── RateLimitFilter
├── scheduler/
│   └── InvestmentReturnScheduler
└── util/
    ├── EmailService
    └── Hash
```

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

---

## 🚢 Production Checklist

- [ ] Set `ddl-auto: validate` and use Flyway/Liquibase for migrations
- [ ] Move rate limiting to Redis for multi-instance deployments
- [ ] Enable HTTPS via load balancer (TLS termination)
- [ ] Set up PostgreSQL backups with point-in-time recovery
- [ ] Add Prometheus + Grafana for monitoring
- [ ] Implement 2FA for high-value transactions
- [ ] Encrypt sensitive fields at rest
- [ ] Add file storage (S3/Cloudinary) for marketplace images
- [ ] Set up WebSocket notifications for order status updates
- [ ] Run penetration testing
- [ ] Set up CI/CD pipeline with automated tests
- [ ] Use environment variables for all secrets (never hardcode)

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 👥 Team

Built with ❤️ by the **Xpenskey** team.

**Contact:** support@xpenskey.com