package dev.rabauer.web;

import dev.rabauer.workflow.VacationRequest;

import java.time.Instant;

public record PendingVacationView(String workflowId, VacationRequest request, String aiSummary, Instant startTime) {
}
