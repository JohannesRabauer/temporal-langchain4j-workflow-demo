package dev.rabauer.web;

import dev.rabauer.workflow.VacationRequest;

import java.time.Instant;
import java.util.List;

public record PendingVacationView(String workflowId, VacationRequest request, List<String> conflicts,
                                   String aiSummary, Instant startTime) {
}
