package dev.rabauer.activity;

import dev.rabauer.workflow.VacationRequest;
import io.temporal.activity.ActivityInterface;

import java.util.List;

@ActivityInterface
public interface VacationConflictActivity {
    List<String> findOverlaps(VacationRequest request);
}
