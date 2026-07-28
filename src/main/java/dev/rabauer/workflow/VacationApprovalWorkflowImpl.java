package dev.rabauer.workflow;

import dev.rabauer.activity.VacationAiActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class VacationApprovalWorkflowImpl implements VacationApprovalWorkflow {

    private final VacationAiActivity aiActivity = Workflow.newActivityStub(
            VacationAiActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                    .build());

    private VacationRequest request;
    private String aiSummary;
    private ApprovalDecision decision;

    @Override
    public VacationDecision run(VacationRequest request) {
        this.request = request;
        this.aiSummary = aiActivity.summarize(request);

        Workflow.await(() -> decision != null);

        return new VacationDecision(decision.approved(), decision.comment(), aiSummary);
    }

    @Override
    public void decide(ApprovalDecision decision) {
        this.decision = decision;
    }

    @Override
    public VacationSnapshot getSnapshot() {
        return new VacationSnapshot(request, aiSummary);
    }
}
