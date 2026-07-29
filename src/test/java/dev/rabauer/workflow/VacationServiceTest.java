package dev.rabauer.workflow;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the pure date-overlap logic the conflict check relies on, without needing Quarkus
 * or the AI services.
 */
class VacationServiceTest {

    private VacationRequest request(String start, String end) {
        return new VacationRequest("Someone", LocalDate.parse(start), LocalDate.parse(end), "reason");
    }

    @Test
    void detectsFullOverlap() {
        assertTrue(VacationService.overlaps(
                request("2026-08-01", "2026-08-10"),
                request("2026-08-03", "2026-08-05")));
    }

    @Test
    void detectsPartialOverlap() {
        assertTrue(VacationService.overlaps(
                request("2026-08-01", "2026-08-10"),
                request("2026-08-08", "2026-08-15")));
    }

    @Test
    void treatsTouchingEndpointsAsOverlapping() {
        assertTrue(VacationService.overlaps(
                request("2026-08-01", "2026-08-05"),
                request("2026-08-05", "2026-08-10")));
    }

    @Test
    void doesNotFlagDisjointRanges() {
        assertFalse(VacationService.overlaps(
                request("2026-08-01", "2026-08-05"),
                request("2026-08-06", "2026-08-10")));
    }
}
