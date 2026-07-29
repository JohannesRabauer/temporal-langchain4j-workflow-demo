package dev.rabauer.web;

import dev.rabauer.workflow.VacationRecord;
import dev.rabauer.workflow.VacationService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the whole stack through its real HTTP endpoints, including the real Ollama-backed AI
 * services. Requires the docker-compose infra (Ollama, with a model already pulled) to be
 * running. Everything here is synchronous, so no polling is needed.
 */
@QuarkusTest
class VacationResourceTest {

    @Inject
    VacationService vacationService;

    @Test
    void submitReviewApproveFullFlow() {
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

        String id = findPendingId(employeeName);
        assertNotNull(id, "submitted request should show up as pending");

        VacationRecord pendingRecord = vacationService.listPending().stream()
                .filter(r -> r.id().equals(id))
                .findFirst()
                .orElseThrow();
        assertNotNull(pendingRecord.aiSummary(), "AI recommendation should already be present");

        given().when().get("/")
                .then().statusCode(200)
                .body(containsString(employeeName));

        given()
                .redirects().follow(false)
                .formParam("approved", "true")
                .formParam("comment", "Approved by test")
                .when().post("/vacations/" + id + "/decide")
                .then().statusCode(303);

        VacationRecord decidedRecord = vacationService.listDecided().stream()
                .filter(r -> r.id().equals(id))
                .findFirst()
                .orElseThrow();
        assertTrue(decidedRecord.decision().approved());
        assertNotNull(decidedRecord.decision().notificationMessage(), "notification should have been drafted");

        given().when().get("/")
                .then().statusCode(200)
                .body(containsString(employeeName))
                .body(containsString("Approved"));
    }

    private String findPendingId(String employeeName) {
        return vacationService.listPending().stream()
                .filter(r -> employeeName.equals(r.request().employeeName()))
                .map(VacationRecord::id)
                .findFirst()
                .orElse(null);
    }
}
