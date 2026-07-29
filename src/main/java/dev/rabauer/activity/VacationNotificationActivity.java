package dev.rabauer.activity;

import dev.rabauer.workflow.ApprovalDecision;
import dev.rabauer.workflow.VacationRequest;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface VacationNotificationActivity {
    String notifyEmployee(VacationRequest request, ApprovalDecision decision);
}
