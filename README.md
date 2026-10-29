# Rupeek SDE-3 Machine Coding — Maid Booking System

A modular, concurrency-safe **Maid (Home-Services) Booking Platform Backend** built with **Java 17** and **Spring Boot 3.2**, designed around Clean / Hexagonal Architecture, SOLID principles, explicit Domain State Machines, and pluggable Strategy/Specification patterns.

---

## 1. Key Engineering Highlights (Evaluation Rubric Mapping)

| Rubric Area | Engineering Decision & Implementation |
| :--- | :--- |
| **Modularity & Clean Architecture** | Strict separation between `domain` (entities, value objects, state machines, ports, strategies, specifications), `application` (orchestration services), `infrastructure` (in-memory & PostgreSQL adapters, lock manager), and `api` (REST controllers, DTOs, RFC-7807 error handling). |
| **Open-Closed Maid Discovery (`Specification` Pattern)** | `MaidSpecification` contract with independent beans (`LocalitySpecification`, `SkillSpecification`, `RatingSpecification`, `MaxPriceSpecification`, `GenderPreferenceSpecification`, `WorkingHoursSpecification`). Adding a new filter requires zero changes to `MaidDiscoveryAndOnboardingService`. |
| **Extensible Booking Types (`Strategy` Pattern)** | `BookingStrategy` abstraction supporting `INSTANT`, `SCHEDULED`, and `RECURRING` bookings. Each strategy expands a request into concrete half-open `TimeSlot`s `[startTime, endTime)`. |
| **Concurrency & Double-Booking Prevention** | Two-tier concurrency protection: (1) In-memory/JVM tier uses `StripedMaidSlotLockManager` (`ConcurrentHashMap<UUID, ReentrantLock>` with fairness and 3s timeout) so simultaneous requests for the same maid serialize cleanly while different maids execute in parallel; (2) PostgreSQL 16 tier uses a GiST Exclusion Constraint (`EXCLUDE USING gist (maid_id WITH =, tstzrange(start_time, end_time, '[)') WITH &&) WHERE (status IN ('PENDING_PAYMENT', 'CONFIRMED'))`) making overlapping active slots impossible at the database kernel level. |
| **Pluggable Payment Gateways & Idempotency** | `PaymentGatewayStrategy` (`UPI`, `CARD`, `WALLET`) + mandatory `Idempotency-Key` request header deduplication and support for both full-series payment and per-occurrence recurring payment. |
| **Cancellation & Refund Policy** | `CancellationRefundPolicy` (`TieredTimeWindowRefundPolicy`: `>=24h` -> 100% refund, `4h-24h` -> 50% refund, `<4h` -> 0% refund). Supports cancelling either a **single occurrence** of a recurring series (`PARTIALLY_CANCELLED`) or the **entire series** (`CANCELLED`), immediately freeing the maid's slot for re-discovery. |

---

## 2. Project Folder Hierarchy

```text
maid-booking-system/
├── .vscode/
│   ├── extensions.json
│   ├── launch.json
│   └── settings.json
├── docs/
│   └── openapi-contract.yaml
├── postman/
│   └── Maid_Booking_System.postman_collection.json
├── scripts/
│   └── publish_to_github.sh
├── src/
│   ├── main/
│   │   ├── java/com/rupeek/maidbooking/
│   │   │   ├── MaidBookingApplication.java
│   │   │   ├── api/
│   │   │   │   ├── controller/
│   │   │   │   │   ├── MaidController.java
│   │   │   │   │   ├── BookingController.java
│   │   │   │   │   └── PaymentController.java
│   │   │   │   ├── dto/
│   │   │   │   │   └── ApiDtos.java
│   │   │   │   └── exception/
│   │   │   │       └── GlobalExceptionHandler.java
│   │   │   ├── application/
│   │   │   │   └── service/
│   │   │   │       ├── MaidDiscoveryAndOnboardingService.java
│   │   │   │       ├── BookingOrchestrationService.java
│   │   │   │       ├── PaymentOrchestrationService.java
│   │   │   │       └── CancellationOrchestrationService.java
│   │   │   ├── domain/
│   │   │   │   ├── exception/
│   │   │   │   │   ├── InvalidStateTransitionException.java
│   │   │   │   │   ├── PaymentProcessingException.java
│   │   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   │   └── SlotConflictException.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── AvailabilityWindow.java
│   │   │   │   │   ├── Booking.java
│   │   │   │   │   ├── BookingOccurrence.java
│   │   │   │   │   ├── BookingStatus.java
│   │   │   │   │   ├── BookingType.java
│   │   │   │   │   ├── Gender.java
│   │   │   │   │   ├── Location.java
│   │   │   │   │   ├── Maid.java
│   │   │   │   │   ├── OccurrenceStatus.java
│   │   │   │   │   ├── Payment.java
│   │   │   │   │   ├── PaymentMethodType.java
│   │   │   │   │   ├── PaymentStatus.java
│   │   │   │   │   ├── RecurrencePattern.java
│   │   │   │   │   ├── RefundResult.java
│   │   │   │   │   ├── ServiceSkill.java
│   │   │   │   │   └── TimeSlot.java
│   │   │   │   ├── port/
│   │   │   │   │   ├── BookingRepository.java
│   │   │   │   │   ├── MaidRepository.java
│   │   │   │   │   ├── MaidSlotLockManager.java
│   │   │   │   │   └── PaymentRepository.java
│   │   │   │   ├── specification/
│   │   │   │   │   ├── GenderPreferenceSpecification.java
│   │   │   │   │   ├── LocalitySpecification.java
│   │   │   │   │   ├── MaidSearchCriteria.java
│   │   │   │   │   ├── MaidSpecification.java
│   │   │   │   │   ├── MaxPriceSpecification.java
│   │   │   │   │   ├── RatingSpecification.java
│   │   │   │   │   ├── SkillSpecification.java
│   │   │   │   │   └── WorkingHoursSpecification.java
│   │   │   │   └── strategy/
│   │   │   │       ├── booking/
│   │   │   │       │   ├── BookingCreationCommand.java
│   │   │   │       │   ├── BookingStrategy.java
│   │   │   │       │   ├── InstantBookingStrategy.java
│   │   │   │       │   ├── RecurringBookingStrategy.java
│   │   │   │       │   └── ScheduledBookingStrategy.java
│   │   │   │       ├── cancellation/
│   │   │   │       │   ├── CancellationRefundPolicy.java
│   │   │   │       │   └── TieredTimeWindowRefundPolicy.java
│   │   │   │       ├── payment/
│   │   │   │       │   ├── CardPaymentStrategy.java
│   │   │   │       │   ├── PaymentGatewayStrategy.java
│   │   │   │       │   ├── UpiPaymentStrategy.java
│   │   │   │       │   └── WalletPaymentStrategy.java
│   │   │   │       └── pricing/
│   │   │   │           ├── DynamicTieredPricingStrategy.java
│   │   │   │           └── PricingStrategy.java
│   │   │   └── infrastructure/
│   │   │       └── persistence/
│   │   │           ├── InMemoryBookingRepository.java
│   │   │           ├── InMemoryMaidRepository.java
│   │   │           ├── InMemoryPaymentRepository.java
│   │   │           └── StripedMaidSlotLockManager.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           └── V1__init_schema.sql
│   └── test/
│       └── java/com/rupeek/maidbooking/
│           └── MaidBookingCoreBusinessTest.java
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## 3. How to Build, Test, and Run

### Option A: Zero-Dependency Local Run (In-Memory Profile)
```bash
# Run unit & concurrency tests
mvn clean test

# Start the Spring Boot server on http://localhost:8080 (Swagger UI at /swagger-ui.html)
mvn spring-boot:run
```

### Option B: Docker Compose (Spring Boot + PostgreSQL 16)
```bash
docker compose up --build -d
docker compose logs -f maid-booking-api
```

### Option C: Import Postman Collection
Import `postman/Maid_Booking_System.postman_collection.json` into Postman (or the VS Code Postman extension) and run requests `1` through `6` sequentially.
