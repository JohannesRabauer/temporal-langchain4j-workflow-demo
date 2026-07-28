package dev.rabauer.activity;

import dev.rabauer.ai.VacationAdvisor;
import dev.rabauer.workflow.VacationRequest;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class VacationAiActivityImpl implements VacationAiActivity {

    @Inject
    VacationAdvisor advisor;

    @Override
    @ActivateRequestContext
    public String summarize(VacationRequest request) {
        return advisor.review(
                request.employeeName(),
                request.startDate().toString(),
                request.endDate().toString(),
                request.reason());
    }
}
