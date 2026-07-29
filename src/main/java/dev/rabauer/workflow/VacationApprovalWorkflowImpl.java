package dev.rabauer.workflow;

import dev.rabauer.activity.VacationAiActivity;
import dev.rabauer.activity.VacationConflictActivity;
import dev.rabauer.activity.VacationNotificationActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.List;

public class VacationApprovalWorkflowImpl implements VacationApprovalWorkflow {

    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(2))
            .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
            .build();

    private final VacationConflictActivity conflictActivity =
            Workflow.newActivityStub(VacationConflictActivity.class, ACTIVITY_OPTIONS);
    private final VacationAiActivity aiActivity =
            Workflow.newActivityStub(VacationAiActivity.class, ACTIVITY_OPTIONS);
    private final VacationNotificationActivity notificationActivity =
            Workflow.newActivityStub(VacationNotificationActivity.class, ACTIVITY_OPTIONS);

    private VacationRequest request;
    private List<String> conflicts = List.of();
    private String aiSummary;
    private ApprovalDecision decision;

    @Override
    public VacationDecision run(VacationRequest request) {
        this.request = request;
        this.conflicts = conflictActivity.findOverlaps(request);
        this.aiSummary = aiActivity.summarize(request, conflicts);

        Workflow.await(() -> decision != null);

        String notificationMessage = notificationActivity.notifyEmployee(request, decision);

        return new VacationDecision(decision.approved(), decision.comment(), aiSummary, notificationMessage);
    }

    @Override
    public void decide(ApprovalDecision decision) {
        this.decision = decision;
    }

    @Override
    public VacationSnapshot getSnapshot() {
        return new VacationSnapshot(request, conflicts, aiSummary);
    }
}
