package dev.rabauer.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface VacationApprovalWorkflow {

    @WorkflowMethod
    VacationDecision run(VacationRequest request);

    @SignalMethod
    void decide(ApprovalDecision decision);

    @QueryMethod
    VacationSnapshot getSnapshot();
}
