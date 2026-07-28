package dev.rabauer.workflow;

import dev.rabauer.activity.VacationAiActivity;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the workflow's control flow in-memory (no real Temporal server or Ollama),
 * using a fake activity implementation.
 */
class VacationApprovalWorkflowTest {

    private static final String TASK_QUEUE = "test-vacation-approval";
    private static final String CANNED_SUMMARY = "Looks reasonable; recommend approval.";

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(VacationApprovalWorkflowImpl.class);
        worker.registerActivitiesImplementations((VacationAiActivity) request -> CANNED_SUMMARY);
        testEnv.start();
        client = testEnv.getWorkflowClient();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    private VacationApprovalWorkflow newStub() {
        return client.newWorkflowStub(
                VacationApprovalWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    }

    private VacationRequest sampleRequest() {
        return new VacationRequest(
                "Ada Lovelace", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), "Family trip");
    }

    @Test
    void waitsForSignalThenApproves() {
        VacationApprovalWorkflow workflow = newStub();
        CompletableFuture<VacationDecision> resultFuture = WorkflowClient.execute(workflow::run, sampleRequest());

        workflow.decide(new ApprovalDecision(true, "Enjoy!"));

        VacationDecision decision = resultFuture.join();
        assertTrue(decision.approved());
        assertEquals("Enjoy!", decision.comment());
        assertEquals(CANNED_SUMMARY, decision.aiSummary());
    }

    @Test
    void waitsForSignalThenRejects() {
        VacationApprovalWorkflow workflow = newStub();
        CompletableFuture<VacationDecision> resultFuture = WorkflowClient.execute(workflow::run, sampleRequest());

        workflow.decide(new ApprovalDecision(false, "Too much overlap with the release."));

        VacationDecision decision = resultFuture.join();
        assertFalse(decision.approved());
        assertEquals("Too much overlap with the release.", decision.comment());
    }

    @Test
    void queryReturnsAiSummaryBeforeDecisionIsMade() {
        VacationApprovalWorkflow workflow = newStub();
        VacationRequest request = sampleRequest();
        WorkflowClient.execute(workflow::run, request);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            VacationSnapshot snapshot = workflow.getSnapshot();
            assertNotNull(snapshot.aiSummary());
            assertEquals(request, snapshot.request());
        });

        workflow.decide(new ApprovalDecision(true, null));
    }
}
