package dev.rabauer.workflow;

import dev.rabauer.ai.VacationAdvisor;
import dev.rabauer.ai.VacationNotifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the vacation approval process end to end, in memory only. There is no persistence here:
 * every pending and decided request lives in these two maps and nothing else. Restarting the
 * app loses everything in {@code pending} — that's the fragility the live session fixes by
 * turning this into a durable Temporal workflow.
 */
@ApplicationScoped
public class VacationService {

    private final Map<String, VacationRecord> pending = new ConcurrentHashMap<>();
    private final Map<String, VacationRecord> decided = new ConcurrentHashMap<>();

    @Inject
    VacationAdvisor advisor;

    @Inject
    VacationNotifier notifier;

    public String submit(VacationRequest request) {
        List<String> conflicts = findConflicts(request);
        String conflictText = conflicts.isEmpty() ? "None" : String.join("; ", conflicts);

        String aiSummary = advisor.review(
                request.employeeName(),
                request.startDate().toString(),
                request.endDate().toString(),
                request.reason(),
                conflictText);

        String id = UUID.randomUUID().toString();
        pending.put(id, new VacationRecord(id, request, conflicts, aiSummary, Instant.now(), null));
        return id;
    }

    public void decide(String id, ApprovalDecision decision) {
        VacationRecord record = pending.remove(id);
        if (record == null) {
            throw new NotFoundException("No pending vacation request with id " + id);
        }

        String managerComment = decision.comment() == null || decision.comment().isBlank()
                ? "None"
                : decision.comment();

        String notificationMessage = notifier.draftMessage(
                record.request().employeeName(),
                record.request().startDate().toString(),
                record.request().endDate().toString(),
                decision.approved() ? "Approved" : "Rejected",
                managerComment);

        VacationDecision finalDecision = new VacationDecision(
                decision.approved(), decision.comment(), record.aiSummary(), notificationMessage);

        decided.put(id, new VacationRecord(
                id, record.request(), record.conflicts(), record.aiSummary(), record.submittedAt(), finalDecision));
    }

    public List<VacationRecord> listPending() {
        return pending.values().stream()
                .sorted(Comparator.comparing(VacationRecord::submittedAt).reversed())
                .toList();
    }

    public List<VacationRecord> listDecided() {
        return decided.values().stream()
                .sorted(Comparator.comparing(VacationRecord::submittedAt).reversed())
                .toList();
    }

    private List<String> findConflicts(VacationRequest request) {
        List<String> conflicts = new ArrayList<>();

        for (VacationRecord other : pending.values()) {
            if (overlaps(request, other.request())) {
                conflicts.add(describe(other.request(), "pending decision"));
            }
        }

        for (VacationRecord other : decided.values()) {
            if (other.decision().approved() && overlaps(request, other.request())) {
                conflicts.add(describe(other.request(), "approved"));
            }
        }

        return conflicts;
    }

    private String describe(VacationRequest other, String status) {
        return "%s (%s): %s to %s".formatted(other.employeeName(), status, other.startDate(), other.endDate());
    }

    static boolean overlaps(VacationRequest a, VacationRequest b) {
        return !a.startDate().isAfter(b.endDate()) && !b.startDate().isAfter(a.endDate());
    }
}
