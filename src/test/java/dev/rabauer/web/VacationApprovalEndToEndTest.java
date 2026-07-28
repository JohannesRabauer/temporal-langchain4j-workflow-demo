package dev.rabauer.web;

import dev.rabauer.workflow.VacationApprovalWorkflow;
import dev.rabauer.workflow.VacationDecision;
import dev.rabauer.workflow.VacationSnapshot;
import io.quarkus.test.junit.QuarkusTest;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the whole stack through its real HTTP endpoints: submitting a request starts a real
 * Temporal workflow, which calls the real Ollama-backed AI activity, before being approved via
 * a real Temporal signal. Requires the docker-compose infra (Temporal + Ollama, with a model
 * already pulled) to be running.
 */
@QuarkusTest
class VacationApprovalEndToEndTest {

    @Inject
    WorkflowClient workflowClient;

    @Test
    void submitReviewApproveFullFlow() throws java.util.concurrent.TimeoutException {
        String employeeName = "E2E Test " + System.currentTimeMillis();
        LocalDate startDate = LocalDate.now().plusDays(30);
        LocalDate endDate = startDate.plusDays(5);

        given()
                .redirects().follow(false)
                .formParam("employeeName", employeeName)
                .formParam("startDate", startDate.toString())
                .formParam("endDate", endDate.toString())
                .formParam("reason", "End-to-end test run")
                .when().post("/vacations")
                .then().statusCode(303);

        String workflowId = findRunningWorkflowId(employeeName);
        assertNotNull(workflowId, "submitted workflow should show up as running");

        VacationApprovalWorkflow stub = workflowClient.newWorkflowStub(VacationApprovalWorkflow.class, workflowId);
        await().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            VacationSnapshot snapshot = stub.getSnapshot();
            assertNotNull(snapshot.aiSummary(), "AI activity should have produced a summary by now");
        });

        given().when().get("/")
                .then().statusCode(200)
                .body(containsString(employeeName));

        given()
                .redirects().follow(false)
                .formParam("approved", "true")
                .formParam("comment", "Approved by e2e test")
                .when().post("/vacations/" + workflowId + "/decide")
                .then().statusCode(303);

        WorkflowStub untyped = workflowClient.newUntypedWorkflowStub(workflowId);
        VacationDecision decision = untyped.getResult(15, TimeUnit.SECONDS, VacationDecision.class);
        assertTrue(decision.approved());

        given().when().get("/")
                .then().statusCode(200)
                .body(containsString(employeeName))
                .body(containsString("Approved"));
    }

    private String findRunningWorkflowId(String employeeName) {
        AtomicReference<String> found = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(300)).until(() ->
                workflowClient.listExecutions("WorkflowType='VacationApprovalWorkflow'")
                        .filter(exec -> exec.getStatus() == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING)
                        .map(exec -> exec.getExecution().getWorkflowId())
                        .anyMatch(id -> {
                            VacationApprovalWorkflow stub =
                                    workflowClient.newWorkflowStub(VacationApprovalWorkflow.class, id);
                            VacationSnapshot snapshot = stub.getSnapshot();
                            boolean matches = snapshot.request() != null
                                    && employeeName.equals(snapshot.request().employeeName());
                            if (matches) {
                                found.set(id);
                            }
                            return matches;
                        }));
        return found.get();
    }
}
