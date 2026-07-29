package dev.rabauer.activity;

import dev.rabauer.ai.VacationNotifier;
import dev.rabauer.workflow.ApprovalDecision;
import dev.rabauer.workflow.VacationRequest;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

@Singleton
public class VacationNotificationActivityImpl implements VacationNotificationActivity {

    private static final Logger LOG = Logger.getLogger(VacationNotificationActivityImpl.class);

    @Inject
    VacationNotifier notifier;

    @Override
    @ActivateRequestContext
    public String notifyEmployee(VacationRequest request, ApprovalDecision decision) {
        String managerComment = decision.comment() == null || decision.comment().isBlank()
                ? "None"
                : decision.comment();

        String message = notifier.draftMessage(
                request.employeeName(),
                request.startDate().toString(),
                request.endDate().toString(),
                decision.approved() ? "Approved" : "Rejected",
                managerComment);

        LOG.infof("Notifying %s: %s", request.employeeName(), message);
        return message;
    }
}
