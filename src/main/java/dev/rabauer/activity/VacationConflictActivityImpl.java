package dev.rabauer.activity;

import dev.rabauer.workflow.VacationApprovalWorkflow;
import dev.rabauer.workflow.VacationDecision;
import dev.rabauer.workflow.VacationRequest;
import dev.rabauer.workflow.VacationSnapshot;
import io.temporal.activity.Activity;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class VacationConflictActivityImpl implements VacationConflictActivity {

    private static final Logger LOG = Logger.getLogger(VacationConflictActivityImpl.class);

    @Inject
    WorkflowClient workflowClient;

    @Override
    public List<String> findOverlaps(VacationRequest request) {
        String ownWorkflowId = Activity.getExecutionContext().getInfo().getWorkflowId();
        List<String> conflicts = new ArrayList<>();

        workflowClient.listExecutions("WorkflowType='VacationApprovalWorkflow'").forEach(execution -> {
            String workflowId = execution.getExecution().getWorkflowId();
            if (workflowId.equals(ownWorkflowId)) {
                return;
            }

            WorkflowExecutionStatus status = execution.getStatus();
            boolean running = status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING;
            boolean completed = status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED;
            if (!running && !completed) {
                return;
            }

            try {
                VacationApprovalWorkflow stub = workflowClient.newWorkflowStub(VacationApprovalWorkflow.class, workflowId);
                VacationSnapshot snapshot = stub.getSnapshot();
                if (snapshot.request() == null || !overlaps(request, snapshot.request())) {
                    return;
                }

                if (running) {
                    conflicts.add(describe(snapshot.request(), "pending decision"));
                } else {
                    WorkflowStub untyped = workflowClient.newUntypedWorkflowStub(workflowId);
                    VacationDecision decision = untyped.getResult(VacationDecision.class);
                    if (decision.approved()) {
                        conflicts.add(describe(snapshot.request(), "approved"));
                    }
                }
            } catch (Exception e) {
                // One unreadable execution (e.g. left over from a previous workflow-code version
                // and no longer replayable) must not block the conflict check for everyone else.
                LOG.warnf(e, "Skipping workflow %s while checking for conflicts", workflowId);
            }
        });

        return conflicts;
    }

    private String describe(VacationRequest other, String status) {
        return "%s (%s): %s to %s".formatted(other.employeeName(), status, other.startDate(), other.endDate());
    }

    static boolean overlaps(VacationRequest a, VacationRequest b) {
        return !a.startDate().isAfter(b.endDate()) && !b.startDate().isAfter(a.endDate());
    }
}
