package dev.rabauer.activity;

import dev.rabauer.ai.VacationAdvisor;
import dev.rabauer.workflow.VacationRequest;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class VacationAiActivityImpl implements VacationAiActivity {

    @Inject
    VacationAdvisor advisor;

    @Override
    @ActivateRequestContext
    public String summarize(VacationRequest request, List<String> conflicts) {
        String conflictText = conflicts.isEmpty() ? "None" : String.join("; ", conflicts);
        return advisor.review(
                request.employeeName(),
                request.startDate().toString(),
                request.endDate().toString(),
                request.reason(),
                conflictText);
    }
}
