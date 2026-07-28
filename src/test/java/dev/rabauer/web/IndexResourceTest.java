package dev.rabauer.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class IndexResourceTest {

    @Test
    void indexPageRenders() {
        given()
                .when().get("/")
                .then()
                .statusCode(200)
                .body(containsString("Vacation Approval"));
    }
}
