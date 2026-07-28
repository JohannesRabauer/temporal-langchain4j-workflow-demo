package dev.rabauer.activity;

import dev.rabauer.workflow.VacationRequest;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface VacationAiActivity {
    String summarize(VacationRequest request);
}
