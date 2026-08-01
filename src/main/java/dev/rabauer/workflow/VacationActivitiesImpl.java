package dev.rabauer.workflow;

import dev.rabauer.ai.VacationAdvisor;
import dev.rabauer.ai.VacationNotifier;
import io.temporal.client.WorkflowClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs outside the workflow thread, so it may safely call the LLM and talk back to the
 * Temporal service. {@link #findConflicts} replaces the old in-memory map scan: it lists other
 * workflow executions of this type via the Visibility API and queries each one's current
 * {@link VacationRecord} snapshot.
 */
@ApplicationScoped
public class VacationActivitiesImpl implements VacationActivities {

    @Inject
    VacationAdvisor advisor;

    @Inject
    VacationNotifier notifier;

    @Inject
    WorkflowClient client;

    @Override
    public List<String> findConflicts(VacationRequest request, String excludeWorkflowId) {
        List<String> conflicts = new ArrayList<>();

        forEachExecution("Running", excludeWorkflowId, other -> {
            if (VacationService.overlaps(request, other.request())) {
                conflicts.add(describe(other.request(), "pending decision"));
            }
        });

        forEachExecution("Completed", excludeWorkflowId, other -> {
            if (other.decision() != null && other.decision().approved()
                    && VacationService.overlaps(request, other.request())) {
                conflicts.add(describe(other.request(), "approved"));
            }
        });

        return conflicts;
    }

    @Override
    @ActivateRequestContext
    public String reviewRequest(VacationRequest request, List<String> conflicts) {
        String conflictText = conflicts.isEmpty() ? "None" : String.join("; ", conflicts);
        return advisor.review(
                request.employeeName(),
                request.startDate().toString(),
                request.endDate().toString(),
                request.reason(),
                conflictText);
    }

    @Override
    @ActivateRequestContext
    public String draftNotification(VacationRequest request, ApprovalDecision decision) {
        String managerComment = decision.comment() == null || decision.comment().isBlank()
                ? "None"
                : decision.comment();

        return notifier.draftMessage(
                request.employeeName(),
                request.startDate().toString(),
                request.endDate().toString(),
                decision.approved() ? "Approved" : "Rejected",
                managerComment);
    }

    private void forEachExecution(String executionStatus, String excludeWorkflowId, java.util.function.Consumer<VacationRecord> consumer) {
        String query = "WorkflowType='%s' AND ExecutionStatus='%s'".formatted(
                VacationApprovalWorkflow.class.getSimpleName(), executionStatus);

        client.listExecutions(query)
                .map(execution -> execution.getExecution().getWorkflowId())
                .filter(workflowId -> !workflowId.equals(excludeWorkflowId))
                .forEach(workflowId -> {
                    VacationRecord other = client.newWorkflowStub(VacationApprovalWorkflow.class, workflowId).getRecord();
                    if (other != null) {
                        consumer.accept(other);
                    }
                });
    }

    private String describe(VacationRequest other, String status) {
        return "%s (%s): %s to %s".formatted(other.employeeName(), status, other.startDate(), other.endDate());
    }
}
