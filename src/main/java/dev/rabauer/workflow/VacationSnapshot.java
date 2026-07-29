package dev.rabauer.workflow;

import java.util.List;

public record VacationSnapshot(VacationRequest request, List<String> conflicts, String aiSummary) {
}
