package dev.rabauer.workflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Entry point the web layer uses to drive the vacation approval process. Every request is now a
 * durable {@link VacationApprovalWorkflow} execution: submitting starts it, deciding sends a
 * signal, and listing pending/decided requests queries the currently running or completed
 * executions via the Temporal Visibility API — nothing is kept in this class's own memory
 * anymore, so a restart of the app loses nothing.
 */
@ApplicationScoped
public class VacationService {

    @Inject
    WorkflowClient client;

    public String submit(VacationRequest request) {
        String workflowId = "vacation-" + UUID.randomUUID();

        VacationApprovalWorkflow workflow = client.newWorkflowStub(
                VacationApprovalWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(VacationApprovalWorkflow.TASK_QUEUE)
                        .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(0).build())
                        .build());

        WorkflowClient.start(workflow::run, request);
        return workflowId;
    }

    public void decide(String id, ApprovalDecision decision) {
        try {
            client.newWorkflowStub(VacationApprovalWorkflow.class, id).decide(decision);
        } catch (WorkflowNotFoundException e) {
            throw new NotFoundException("No pending vacation request with id " + id, e);
        }
    }

    public List<VacationRecord> listPending() {
        return listByStatus("Running");
    }

    public List<VacationRecord> listDecided() {
        return listByStatus("Completed");
    }

    private List<VacationRecord> listByStatus(String executionStatus) {
        String query = "WorkflowType='%s' AND ExecutionStatus='%s'".formatted(
                VacationApprovalWorkflow.class.getSimpleName(), executionStatus);

        return client.listExecutions(query)
                .map(execution -> execution.getExecution().getWorkflowId())
                .map(workflowId -> client.newWorkflowStub(VacationApprovalWorkflow.class, workflowId).getRecord())
                .sorted(Comparator.comparing(VacationRecord::submittedAt).reversed())
                .toList();
    }

    static boolean overlaps(VacationRequest a, VacationRequest b) {
        return !a.startDate().isAfter(b.endDate()) && !b.startDate().isAfter(a.endDate());
    }
}
