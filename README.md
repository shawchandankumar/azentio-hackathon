# Sentinel AML — Real-Time Money Laundering Detection Platform

Sentinel is a Transaction Monitoring System (TMS) prototype for **MeridianTrust Bank**. It
ingests customer, account and transaction data, runs a **configurable rule engine** over it,
produces **risk-scored, explainable alerts**, and gives compliance analysts a **case-management
workflow** to act on them — with a full **immutable audit trail** and **role-based access control**.

> Design philosophy (per the CCO): *reliably catch known laundering typologies, explain **why**
> something was flagged, and give analysts a clean workflow to act on it.*

---

## 1. Tech stack

| Concern        | Choice                                            |
|----------------|---------------------------------------------------|
| Language       | Java 21                                            |
| Framework      | Spring Boot 3.5 (Web MVC, Data JPA, Security, Actuator) |
| Database       | PostgreSQL 16 (H2 for tests)                        |
| API docs       | springdoc-openapi (Swagger UI)                     |
| Build          | Maven                                              |
| Tests          | JUnit 5 + Mockito + AssertJ                         |

---

## 2. Architecture

Clean layered architecture — **controller → service → repository** — with an isolated
**detection engine**:

```
com.meridiantrust.sentinel
├── domain/            JPA entities (Customer, Account, Transaction, Alert, CaseFile,
│   └── enums/         AlertEvidence, AuditEvent, RuleConfig, ExchangeRate, ...) + enums
├── repository/        Spring Data JPA repositories
├── detection/         Rule engine core
│   ├── DetectionRule  (interface)   RuleEngine   RiskScoring   RuleHit   RuleParams
│   └── rules/         LargeTransaction, Structuring, RapidMovement,
│                      HighRiskJurisdiction, BehavioralDeviation, RoundNumber
├── service/           Ingestion, Detection, Alert, Case, Audit, Currency,
│                      ReferenceData, RuleConfig, Pii masking, Customer, ViewMapper
├── web/               REST controllers (versioned /api/v1) + GlobalExceptionHandler
├── config/            SecurityConfig, OpenApiConfig
├── security/          SecurityUtils (acting-user resolution)
└── bootstrap/         DataSeeder (reference + synthetic data, runs detection)
```

**Request flow (streaming):**
`POST /ingestion/transactions/stream` → `IngestionService` (validate, FX-normalize, persist)
→ `DetectionService` → `RuleEngine` (each enabled rule) → `AlertService`
(score, de-dup/aggregate, persist) → `AuditService` (immutable log).

### Data model (ERD)

```
Customer 1───* Account 1───* Transaction
   │                              │
   │ 1                            │ *  (evidence)
   ▼ *                            ▼
 Alert *───────────────── AlertEvidence
   │  \
   │   *──── CaseFile 1───* Alert           (alerts link into a case)
   ▼
 (rule_type, risk_score, severity, status, disposition, dedup_key, explanation)

Reference/config: RuleConfig, ExchangeRate, HighRiskJurisdiction, WatchlistCounterparty
Audit: AuditEvent (append-only)
```

The canonical SQL schema and reference data are in
[`src/main/resources/db/migration`](src/main/resources/db/migration) (`V1__core_schema.sql`,
`V2__reference_seed.sql`) as documented, Flyway-compatible migration scripts. In this build the
running schema is created by Hibernate and populated by `DataSeeder` so it works out of the box on
both PostgreSQL and H2 (see [§8 Build notes](#8-build-notes)).

---

## 3. Detection rules ↔ business rules

Each triggered rule produces an **Alert** with a risk score, the triggering rule(s), supporting
transaction evidence, and a human-readable explanation. Thresholds/windows live in the
`rule_config` table and are tunable at runtime (no redeploy).

| Rule code | Typology | Business rule | Default configuration |
|-----------|----------|---------------|-----------------------|
| `R1_LARGE_TXN` | Large transaction (CTR) | **#1** single txn ≥ threshold | `thresholdBase` = 831,960 INR (≈ $10k) |
| `R2_STRUCTURING` | Structuring / smurfing | **#2** ≥3 sub-threshold txns / 24h | `minCount`=3, `windowHours`=24, band 748,764–831,876 |
| `R3_RAPID_MOVEMENT` | Rapid movement (layering) | **#3** ≥80% out within 48h | `outflowRatio`=0.80, `windowHours`=48, `minCreditBase`=415,980 |
| `R4_HIGH_RISK_JURISDICTION` | Sanctions / high-risk geo | **#4** any amount | uses `high_risk_jurisdictions` + `watchlist_counterparties` |
| `R5_BEHAVIORAL_DEVIATION` | Unusual volume | **#5** daily > 3× 90-day avg | `multiplier`=3.0, `lookbackDays`=90, `minBaselineBase`=41,598 |
| `R6_ROUND_NUMBER` | Round-number pattern | (round/just-below) | `minCount`=3, `windowHours`=72, `roundUnit`=1,000 |

**Risk score (0–100, Business Rule 7):** each rule contributes a configurable `baseWeight`,
adjusted by the customer's KYC risk rating. When multiple rules reinforce the same underlying
pattern, scores combine (higher + diminishing fraction of the lower) and the reinforcing rule codes
are recorded on the alert. Higher scores sort to the top of the analyst queue.

**De-duplication / aggregation:** every hit has a `dedupKey` identifying the underlying pattern
(e.g. `STRUCT:<accountId>:<date>`). A repeat detection merges into the existing open alert
(adding evidence, re-scoring) instead of creating a redundant one — so one customer doesn't
generate 50 alerts for the same behaviour.

**Never silently deleted (Business Rule 6):** alerts are only ever transitioned to `CLEARED` /
`CONFIRMED` with a disposition reason and the analyst's identity, all captured in `audit_events`.

---

## 4. Security & roles

Stateless **HTTP Basic** auth with RBAC enforced at the API layer via `@PreAuthorize`
(not just the UI). Bootstrap credentials come from environment variables (no hardcoded secrets).

| Role | Capabilities |
|------|--------------|
| `ADMIN` | ingestion, rule config, reference data (FX / sanctions lists), bulk detection |
| `SUPERVISOR` | full analyst workflow **+ unmasked PII** in detail views |
| `ANALYST` | alert queue, disposition, cases (PII masked) |

**PII masking (Business Rule 8):** customer names/IDs are masked in list views for everyone and
in detail views for `ANALYST`; only `SUPERVISOR`/`ADMIN` see full PII in detail views.

Default local users (override the passwords via env in any real deployment):

| User | Password env var | Default |
|------|------------------|---------|
| `admin` | `SENTINEL_ADMIN_PASSWORD` | `admin123` |
| `supervisor` | `SENTINEL_SUPERVISOR_PASSWORD` | `supervisor123` |
| `analyst` | `SENTINEL_ANALYST_PASSWORD` | `analyst123` |

---

## 5. Running it

### Prerequisites
Java 21, Maven, Docker (for PostgreSQL).

### Start the database
```bash
docker compose up -d          # PostgreSQL 16 on :5432
```

### Configure secrets (optional — sensible defaults exist for local dev)
```bash
cp .env.example .env          # then export the vars, or set them in your shell/CI
```

### Run the app
```bash
mvn spring-boot:run
```
On first start with an empty database, `DataSeeder` loads reference data + synthetic customers /
accounts / transactions covering **all six typologies**, then runs detection — so alerts exist
immediately.

- Swagger UI: **http://localhost:8080/swagger-ui.html**
- OpenAPI spec: **http://localhost:8080/v3/api-docs**
- Health: **http://localhost:8080/actuator/health**

---

## 6. Demo walkthrough (ingestion → detection → alert → case)

```bash
# 1. View the alert queue (top-risk first). Analyst can log in.
curl -u analyst:analyst123 "http://localhost:8080/api/v1/alerts?status=OPEN"

# 2. Inspect one alert's evidence and explanation
curl -u supervisor:supervisor123 http://localhost:8080/api/v1/alerts/ALT-XXXXXXXX

# 3. Stream a brand-new suspicious transaction and detect inline (admin)
curl -u admin:admin123 -X POST http://localhost:8080/api/v1/ingestion/transactions/stream \
  -H 'Content-Type: application/json' -d '{
    "txnRef":"TXN-DEMO-1","accountNumber":"ACC-2004","direction":"DEBIT","txnType":"TRANSFER",
    "amount":9000,"currency":"USD","counterpartyName":"Crescent Trading FZE",
    "counterpartyCountry":"IR","channel":"WIRE","jurisdiction":"IR",
    "bookedAt":"2026-09-19T10:15:00Z"}'

# 4. Open a case from an alert
curl -u analyst:analyst123 -X POST http://localhost:8080/api/v1/cases \
  -H 'Content-Type: application/json' -d '{
    "customerRef":"CUST-1004","title":"Sanctions exposure - Carlos Mendez",
    "priority":"HIGH","alertRefs":["ALT-XXXXXXXX"]}'

# 5. Disposition the alert (retained for audit, never deleted)
curl -u analyst:analyst123 -X POST http://localhost:8080/api/v1/alerts/ALT-XXXXXXXX/disposition \
  -H 'Content-Type: application/json' -d '{"disposition":"ESCALATED_SAR","reason":"Confirmed sanctioned counterparty"}'

# 6. Review the immutable audit trail
curl -u supervisor:supervisor123 http://localhost:8080/api/v1/alerts/ALT-XXXXXXXX/audit

# 7. Retune a rule at runtime — no redeploy (admin)
curl -u admin:admin123 -X PUT http://localhost:8080/api/v1/rules/R1_LARGE_TXN \
  -H 'Content-Type: application/json' -d '{"paramsJson":"{\"thresholdBase\": 500000.00}"}'

# 8. Re-run detection over all transactions in bulk
curl -u admin:admin123 -X POST http://localhost:8080/api/v1/rules/detect/run
```

---

## 7. API surface (all under `/api/v1`)

| Area | Endpoints |
|------|-----------|
| Ingestion (ADMIN) | `POST /ingestion/customers`, `/ingestion/accounts`, `/ingestion/transactions?detect=`, `/ingestion/transactions/stream` |
| Alerts | `GET /alerts` (queue, risk-sorted), `GET /alerts/{ref}`, `GET /alerts/{ref}/audit`, `POST /alerts/{ref}/disposition`, `PATCH /alerts/{ref}/status` |
| Cases | `POST /cases`, `GET /cases/{ref}`, `GET /cases/{ref}/audit`, `POST /cases/{ref}/alerts/{alertRef}`, `PATCH /cases/{ref}/assign`, `PATCH /cases/{ref}/status`, `POST /cases/{ref}/close` |
| Customers | `GET /customers/{ref}`, `GET /customers/{ref}/transactions` (timeline), `GET /customers/{ref}/alerts` |
| Rules | `GET /rules`, `GET /rules/{code}`, `PUT /rules/{code}` (ADMIN), `POST /rules/detect/run` (ADMIN) |
| Admin | `GET/PUT /admin/exchange-rates`, `GET/POST /admin/jurisdictions` |

All endpoints use proper HTTP status codes, bean-validated request bodies, and a consistent JSON
error envelope (`ApiError`).

---

## 8. Build notes

The delivered `pom.xml` uses standard Spring Boot 3.5.17 managed dependency versions, which resolve
normally from your Maven repository. This build **does not use Flyway at runtime** — the schema is
created by Hibernate (`spring.jpa.hibernate.ddl-auto=update`) and seeded by `DataSeeder` — so it runs
fully offline against PostgreSQL and H2 without extra tooling. The Flyway-style SQL migration scripts
under `db/migration` remain as the canonical, reviewable schema definition (apply them with `psql` or
re-enable Flyway if you prefer DB-managed migrations).

---

## 9. Testing

```bash
mvn test
```
Unit tests (JUnit 5 + Mockito) cover every detection rule (positive/negative/boundary cases), risk
scoring, currency normalization and PII masking — see
[`src/test/java`](src/test/java/com/meridiantrust/sentinel).

---

## 10. Non-functional highlights

- **Performance:** windowed rule queries are index-backed (`transactions(account_id, booked_at)`);
  `POST /rules/detect/run` re-evaluates the whole book for bulk loads.
- **Concurrency:** detection/ingestion are transactional; alert de-dup is guarded by a unique
  `dedup_key` constraint so concurrent streams don't create duplicate alerts.
- **Auditability:** every alert/case transition writes an append-only `audit_events` row with actor
  and timestamp.
- **Data privacy:** all seed data is synthetic; no real PII.
