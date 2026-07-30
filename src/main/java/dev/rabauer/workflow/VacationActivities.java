package dev.rabauer.workflow;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;

/**
 * The non-deterministic work of the vacation approval process: looking at other requests
 * (via the Temporal Visibility API) and asking the LLM for a recommendation or a notification
 * draft. Each method is retried automatically by Temporal on failure.
 */
@ActivityInterface
public interface VacationActivities {

    @ActivityMethod
    List<String> findConflicts(VacationRequest request, String excludeWorkflowId);

    @ActivityMethod
    String reviewRequest(VacationRequest request, List<String> conflicts);

    @ActivityMethod
    String draftNotification(VacationRequest request, ApprovalDecision decision);
}
