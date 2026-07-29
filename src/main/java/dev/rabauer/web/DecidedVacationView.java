package dev.rabauer.web;

import dev.rabauer.workflow.VacationDecision;
import dev.rabauer.workflow.VacationRequest;

import java.time.Instant;

public record DecidedVacationView(String requestId, VacationRequest request, VacationDecision decision, Instant submittedAt) {
}
