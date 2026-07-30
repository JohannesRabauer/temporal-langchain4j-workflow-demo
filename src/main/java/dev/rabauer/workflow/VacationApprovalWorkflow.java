package dev.rabauer.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * The vacation approval process as a durable Temporal workflow. Submitting a request starts an
 * execution; it stays open — surviving worker restarts, crashes, and redeploys — until a
 * manager sends the {@link #decide} signal. {@link #getRecord} lets the web layer read the
 * current state (pending or decided) without waiting for the workflow to complete.
 */
@WorkflowInterface
public interface VacationApprovalWorkflow {

    String TASK_QUEUE = "vacation-approval";

    @WorkflowMethod
    VacationRecord run(VacationRequest request);

    @SignalMethod
    void decide(ApprovalDecision decision);

    @QueryMethod
    VacationRecord getRecord();
}
