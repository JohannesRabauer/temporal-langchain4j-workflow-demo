package dev.rabauer.workflow;

import java.time.Instant;
import java.util.List;

/**
 * A vacation request's in-memory state. {@code decision} is null while the request is pending.
 */
public record VacationRecord(String id, VacationRequest request, List<String> conflicts, String aiSummary,
                              Instant submittedAt, VacationDecision decision) {
}
