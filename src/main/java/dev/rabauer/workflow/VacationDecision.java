package dev.rabauer.workflow;

public record VacationDecision(boolean approved, String comment, String aiSummary, String notificationMessage) {
}
