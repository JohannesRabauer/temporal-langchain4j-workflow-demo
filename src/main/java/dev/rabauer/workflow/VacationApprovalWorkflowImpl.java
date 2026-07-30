package dev.rabauer.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class VacationApprovalWorkflowImpl implements VacationApprovalWorkflow {

    private final VacationActivities activities = Workflow.newActivityStub(
            VacationActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(60))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                    .build());

    private VacationRecord record;
    private ApprovalDecision decision;

    /**
     * Runs before the workflow method and before any signal/query can be delivered, so
     * {@link #getRecord()} always has something sane to return — even while the conflict-check
     * and AI-review activities for this very request are still in flight.
     */
    @WorkflowInit
    public VacationApprovalWorkflowImpl(VacationRequest request) {
        this.record = new VacationRecord(
                Workflow.getInfo().getWorkflowId(), request, List.of(), null,
                Instant.ofEpochMilli(Workflow.currentTimeMillis()), null);
    }

    @Override
    public VacationRecord run(VacationRequest request) {
        String workflowId = Workflow.getInfo().getWorkflowId();

        List<String> conflicts = activities.findConflicts(request, workflowId);
        String aiSummary = activities.reviewRequest(request, conflicts);
        record = new VacationRecord(workflowId, request, conflicts, aiSummary, record.submittedAt(), null);

        Workflow.await(() -> decision != null);

        String notificationMessage = activities.draftNotification(request, decision);
        VacationDecision finalDecision = new VacationDecision(
                decision.approved(), decision.comment(), aiSummary, notificationMessage);
        record = new VacationRecord(workflowId, request, conflicts, aiSummary, record.submittedAt(), finalDecision);

        return record;
    }

    @Override
    public void decide(ApprovalDecision decision) {
        this.decision = decision;
    }

    @Override
    public VacationRecord getRecord() {
        return record;
    }
}
