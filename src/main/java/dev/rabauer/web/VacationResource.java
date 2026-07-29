package dev.rabauer.web;

import dev.rabauer.workflow.ApprovalDecision;
import dev.rabauer.workflow.VacationRequest;
import dev.rabauer.workflow.VacationService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Path("/")
public class VacationResource {

    @Inject
    Template index;

    @Inject
    Template vacationLists;

    @Inject
    VacationService vacationService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return index.data("pending", pendingViews()).data("decided", decidedViews());
    }

    @GET
    @Path("/vacations/fragment")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance fragment() {
        return vacationLists.data("pending", pendingViews()).data("decided", decidedViews());
    }

    private List<PendingVacationView> pendingViews() {
        return vacationService.listPending().stream()
                .map(r -> new PendingVacationView(r.id(), r.request(), r.conflicts(), r.aiSummary(), r.submittedAt()))
                .toList();
    }

    private List<DecidedVacationView> decidedViews() {
        return vacationService.listDecided().stream()
                .map(r -> new DecidedVacationView(r.id(), r.request(), r.decision(), r.submittedAt()))
                .toList();
    }

    @POST
    @Path("/vacations")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response submit(@RestForm String employeeName,
                            @RestForm LocalDate startDate,
                            @RestForm LocalDate endDate,
                            @RestForm String reason) {
        vacationService.submit(new VacationRequest(employeeName, startDate, endDate, reason));
        return Response.seeOther(URI.create("/")).build();
    }

    @POST
    @Path("/vacations/{id}/decide")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response decide(@PathParam("id") String id,
                            @RestForm boolean approved,
                            @RestForm String comment) {
        vacationService.decide(id, new ApprovalDecision(approved, comment));
        return Response.seeOther(URI.create("/")).build();
    }
}
