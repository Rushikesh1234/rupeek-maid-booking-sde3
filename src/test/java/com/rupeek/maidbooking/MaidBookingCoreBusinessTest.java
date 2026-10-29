package com.rupeek.maidbooking;

import com.rupeek.maidbooking.application.service.BookingOrchestrationService;
import com.rupeek.maidbooking.application.service.CancellationOrchestrationService;
import com.rupeek.maidbooking.application.service.CancellationOrchestrationService.CancellationOutcome;
import com.rupeek.maidbooking.application.service.MaidDiscoveryAndOnboardingService;
import com.rupeek.maidbooking.application.service.PaymentOrchestrationService;
import com.rupeek.maidbooking.application.service.PaymentOrchestrationService.PaymentCommand;
import com.rupeek.maidbooking.domain.exception.InvalidStateTransitionException;
import com.rupeek.maidbooking.domain.exception.SlotConflictException;
import com.rupeek.maidbooking.domain.model.AvailabilityWindow;
import com.rupeek.maidbooking.domain.model.Booking;
import com.rupeek.maidbooking.domain.model.BookingStatus;
import com.rupeek.maidbooking.domain.model.BookingType;
import com.rupeek.maidbooking.domain.model.Gender;
import com.rupeek.maidbooking.domain.model.Location;
import com.rupeek.maidbooking.domain.model.Maid;
import com.rupeek.maidbooking.domain.model.OccurrenceStatus;
import com.rupeek.maidbooking.domain.model.Payment;
import com.rupeek.maidbooking.domain.model.PaymentMethodType;
import com.rupeek.maidbooking.domain.model.PaymentStatus;
import com.rupeek.maidbooking.domain.model.RecurrencePattern;
import com.rupeek.maidbooking.domain.model.ServiceSkill;
import com.rupeek.maidbooking.domain.model.TimeSlot;
import com.rupeek.maidbooking.domain.specification.GenderPreferenceSpecification;
import com.rupeek.maidbooking.domain.specification.LocalitySpecification;
import com.rupeek.maidbooking.domain.specification.MaidSearchCriteria;
import com.rupeek.maidbooking.domain.specification.MaxPriceSpecification;
import com.rupeek.maidbooking.domain.specification.RatingSpecification;
import com.rupeek.maidbooking.domain.specification.SkillSpecification;
import com.rupeek.maidbooking.domain.specification.WorkingHoursSpecification;
import com.rupeek.maidbooking.domain.strategy.booking.BookingCreationCommand;
import com.rupeek.maidbooking.domain.strategy.booking.InstantBookingStrategy;
import com.rupeek.maidbooking.domain.strategy.booking.RecurringBookingStrategy;
import com.rupeek.maidbooking.domain.strategy.booking.ScheduledBookingStrategy;
import com.rupeek.maidbooking.domain.strategy.cancellation.TieredTimeWindowRefundPolicy;
import com.rupeek.maidbooking.domain.strategy.payment.CardPaymentStrategy;
import com.rupeek.maidbooking.domain.strategy.payment.UpiPaymentStrategy;
import com.rupeek.maidbooking.domain.strategy.payment.WalletPaymentStrategy;
import com.rupeek.maidbooking.domain.strategy.pricing.DynamicTieredPricingStrategy;
import com.rupeek.maidbooking.infrastructure.persistence.InMemoryBookingRepository;
import com.rupeek.maidbooking.infrastructure.persistence.InMemoryMaidRepository;
import com.rupeek.maidbooking.infrastructure.persistence.InMemoryPaymentRepository;
import com.rupeek.maidbooking.infrastructure.persistence.StripedMaidSlotLockManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pinpoints all SDE-3 core business rules, edge cases, state transitions,
 * idempotency, recurring single-occurrence cancellation, and multithreaded concurrency.
 */
class MaidBookingCoreBusinessTest {

    private MaidDiscoveryAndOnboardingService maidService;
    private BookingOrchestrationService bookingService;
    private PaymentOrchestrationService paymentService;
    private CancellationOrchestrationService cancellationService;
    private Clock fixedClock;

    private Maid lakshmi;
    private Maid ramesh;

    @BeforeEach
    void setUp() {
        // Fixed clock: Thursday, 2026-10-29 10:00:00 IST
        fixedClock = Clock.fixed(Instant.parse("2026-10-29T04:30:00Z"), ZoneId.of("Asia/Kolkata"));

        var maidRepo = new InMemoryMaidRepository();
        var bookingRepo = new InMemoryBookingRepository();
        var paymentRepo = new InMemoryPaymentRepository();
        var lockManager = new StripedMaidSlotLockManager();

        var specs = List.of(
                new LocalitySpecification(),
                new SkillSpecification(),
                new RatingSpecification(),
                new MaxPriceSpecification(),
                new GenderPreferenceSpecification(),
                new WorkingHoursSpecification()
        );

        var bookingStrategies = List.of(
                new InstantBookingStrategy(),
                new ScheduledBookingStrategy(),
                new RecurringBookingStrategy()
        );

        var paymentGateways = List.of(
                new UpiPaymentStrategy(),
                new CardPaymentStrategy(),
                new WalletPaymentStrategy()
        );

        maidService = new MaidDiscoveryAndOnboardingService(maidRepo, bookingRepo, specs);
        bookingService = new BookingOrchestrationService(
                maidRepo, bookingRepo, lockManager, new DynamicTieredPricingStrategy(), bookingStrategies, fixedClock);
        paymentService = new PaymentOrchestrationService(
                bookingRepo, paymentRepo, paymentGateways, fixedClock);
        cancellationService = new CancellationOrchestrationService(
                bookingRepo, paymentRepo, lockManager, new TieredTimeWindowRefundPolicy(), paymentGateways, fixedClock);

        List<AvailabilityWindow> allDays8To20 = List.of(
                new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                new AvailabilityWindow(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                new AvailabilityWindow(DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                new AvailabilityWindow(DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                new AvailabilityWindow(DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                new AvailabilityWindow(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                new AvailabilityWindow(DayOfWeek.SUNDAY, LocalTime.of(8, 0), LocalTime.of(20, 0))
        );

        lakshmi = maidService.onboardMaid(new Maid(
                UUID.randomUUID(),
                "Lakshmi Devi",
                Gender.FEMALE,
                new Location("Indiranagar", "Bengaluru", "560038", 12.9784, 77.6408),
                EnumSet.of(ServiceSkill.CLEANING, ServiceSkill.COOKING, ServiceSkill.DISHWASHING),
                new BigDecimal("250.00"),
                4.8,
                allDays8To20,
                true
        ));

        ramesh = maidService.onboardMaid(new Maid(
                UUID.randomUUID(),
                "Ramesh Kumar",
                Gender.MALE,
                new Location("Indiranagar", "Bengaluru", "560038", 12.9784, 77.6408),
                EnumSet.of(ServiceSkill.CLEANING),
                new BigDecimal("180.00"),
                4.2,
                allDays8To20,
                true
        ));
    }

    @Test
    @DisplayName("1. Discovery filters maids by locality, skill, rating, maxPrice, and gender preference")
    void testMaidDiscoveryWithFilters() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 10, 30, 11, 0),
                LocalDateTime.of(2026, 10, 30, 13, 0));

        MaidSearchCriteria femaleCookingFilter = new MaidSearchCriteria(
                "Indiranagar", ServiceSkill.COOKING, slot, 4.5, new BigDecimal("300.00"), Gender.FEMALE);

        List<Maid> results = maidService.searchAvailableMaids(femaleCookingFilter);
        assertEquals(1, results.size());
        assertEquals(lakshmi.getId(), results.get(0).getId());
    }

    @Test
    @DisplayName("2. Scheduled booking blocks overlapping slots and hides maid from discovery for that window")
    void testScheduledBookingPreventsDoubleBookingAndExcludesFromDiscovery() {
        TimeSlot slot11To13 = new TimeSlot(
                LocalDateTime.of(2026, 10, 30, 11, 0),
                LocalDateTime.of(2026, 10, 30, 13, 0));

        bookingService.createBooking(new BookingCreationCommand(
                UUID.randomUUID(), lakshmi.getId(), ServiceSkill.CLEANING,
                BookingType.SCHEDULED, null, slot11To13, null));

        // Overlapping slot [12:00, 14:00) must fail with SlotConflictException
        TimeSlot overlappingSlot = new TimeSlot(
                LocalDateTime.of(2026, 10, 30, 12, 0),
                LocalDateTime.of(2026, 10, 30, 14, 0));

        assertThrows(SlotConflictException.class, () -> bookingService.createBooking(
                new BookingCreationCommand(
                        UUID.randomUUID(), lakshmi.getId(), ServiceSkill.CLEANING,
                        BookingType.SCHEDULED, null, overlappingSlot, null)));

        // Discovery for [12:00, 14:00) must exclude Lakshmi and only return Ramesh
        List<Maid> available = maidService.searchAvailableMaids(new MaidSearchCriteria(
                "Indiranagar", ServiceSkill.CLEANING, overlappingSlot, null, null, Gender.ANY));
        assertEquals(1, available.size());
        assertEquals(ramesh.getId(), available.get(0).getId());
    }

    @Test
    @DisplayName("3. Recurring booking creates occurrences; cancelling a single occurrence frees only that slot")
    void testRecurringBookingAndSingleOccurrenceCancellation() {
        RecurrencePattern everyMonWedFri = new RecurrencePattern(
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalTime.of(9, 0),
                120,
                LocalDate.of(2026, 11, 2),  // Monday
                LocalDate.of(2026, 11, 6)   // Friday (3 occurrences: Nov 2, Nov 4, Nov 6)
        );

        Booking recurring = bookingService.createBooking(new BookingCreationCommand(
                UUID.randomUUID(), lakshmi.getId(), ServiceSkill.COOKING,
                BookingType.RECURRING, null, null, everyMonWedFri));

        assertEquals(3, recurring.getOccurrences().size());

        // Pay for the entire recurring series via UPI
        paymentService.processPayment(new PaymentCommand(
                recurring.getId(), null, PaymentMethodType.UPI, "customer@okaxis", "IDEMP-REC-1"));
        assertEquals(BookingStatus.CONFIRMED, recurring.getStatus());

        // Cancel only the Wednesday (Nov 4) occurrence (>24h away -> 100% refund for that occurrence)
        UUID wednesdayOccurrenceId = recurring.getOccurrences().get(1).getId();
        CancellationOutcome outcome = cancellationService.cancelSingleOccurrence(
                recurring.getId(), wednesdayOccurrenceId);

        assertEquals(BookingStatus.PARTIALLY_CANCELLED, outcome.updatedBooking().getStatus());
        assertEquals(100, outcome.refundResult().refundPercentage());
        assertEquals(OccurrenceStatus.CANCELLED, recurring.findOccurrence(wednesdayOccurrenceId).getStatus());

        // Now Wednesday Nov 4 [09:00, 11:00) must be discoverable and bookable again for Lakshmi!
        TimeSlot wednesdaySlot = new TimeSlot(
                LocalDateTime.of(2026, 11, 4, 9, 0),
                LocalDateTime.of(2026, 11, 4, 11, 0));
        List<Maid> discovered = maidService.searchAvailableMaids(new MaidSearchCriteria(
                "Indiranagar", ServiceSkill.COOKING, wednesdaySlot, null, null, Gender.FEMALE));
        assertEquals(1, discovered.size());
        assertEquals(lakshmi.getId(), discovered.get(0).getId());
    }

    @Test
    @DisplayName("4. Payment Idempotency-Key prevents duplicate gateway charges on network retries")
    void testPaymentIdempotency() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 10, 31, 14, 0),
                LocalDateTime.of(2026, 10, 31, 16, 0));

        Booking booking = bookingService.createBooking(new BookingCreationCommand(
                UUID.randomUUID(), lakshmi.getId(), ServiceSkill.CLEANING,
                BookingType.SCHEDULED, null, slot, null));

        Payment firstCall = paymentService.processPayment(new PaymentCommand(
                booking.getId(), null, PaymentMethodType.CARD, "4111-XXXX-XXXX-1111", "RETRY-SAFE-KEY-001"));

        Payment secondRetryCall = paymentService.processPayment(new PaymentCommand(
                booking.getId(), null, PaymentMethodType.CARD, "4111-XXXX-XXXX-1111", "RETRY-SAFE-KEY-001"));

        assertEquals(firstCall.getId(), secondRetryCall.getId());
        assertEquals(firstCall.getGatewayReference(), secondRetryCall.getGatewayReference());
        assertEquals(PaymentStatus.SUCCESS, secondRetryCall.getStatus());
    }

    @Test
    @DisplayName("5. Invalid state transition: cancelling an already cancelled booking throws InvalidStateTransitionException")
    void testInvalidStateTransitionRejected() {
        TimeSlot slot = new TimeSlot(
                LocalDateTime.of(2026, 10, 31, 15, 0),
                LocalDateTime.of(2026, 10, 31, 17, 0));

        Booking booking = bookingService.createBooking(new BookingCreationCommand(
                UUID.randomUUID(), lakshmi.getId(), ServiceSkill.CLEANING,
                BookingType.SCHEDULED, null, slot, null));

        cancellationService.cancelEntireBooking(booking.getId());

        assertThrows(InvalidStateTransitionException.class,
                () -> cancellationService.cancelEntireBooking(booking.getId()));
    }

    @Test
    @DisplayName("6. Concurrency stress test: 20 simultaneous threads booking the same maid & slot results in exactly 1 winner and 19 conflicts")
    void testConcurrentBookingOnSameMaidInventory() throws InterruptedException {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        TimeSlot contestedSlot = new TimeSlot(
                LocalDateTime.of(2026, 11, 1, 10, 0),
                LocalDateTime.of(2026, 11, 1, 12, 0));

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    bookingService.createBooking(new BookingCreationCommand(
                            UUID.randomUUID(),
                            lakshmi.getId(),
                            ServiceSkill.CLEANING,
                            BookingType.SCHEDULED,
                            null,
                            contestedSlot,
                            null));
                    successCount.incrementAndGet();
                } catch (SlotConflictException ex) {
                    conflictCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // Release all 20 threads at the exact same instant
        doneLatch.await();
        pool.shutdown();

        assertEquals(1, successCount.get(), "Exactly 1 thread must succeed in booking the slot");
        assertEquals(threads - 1, conflictCount.get(), "All other 19 threads must receive SlotConflictException");
    }
}
