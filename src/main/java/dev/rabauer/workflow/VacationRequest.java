package dev.rabauer.workflow;

import java.time.LocalDate;

public record VacationRequest(String employeeName, LocalDate startDate, LocalDate endDate, String reason) {
}
