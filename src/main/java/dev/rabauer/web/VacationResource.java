package dev.rabauer.web;

import dev.rabauer.workflow.ApprovalDecision;
import dev.rabauer.workflow.VacationApprovalWorkflow;
import dev.rabauer.workflow.VacationDecision;
import dev.rabauer.workflow.VacationRequest;
import dev.rabauer.workflow.VacationSnapshot;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Path("/")
public class VacationResource {

    private static final Logger LOG = Logger.getLogger(VacationResource.class);

    @Inject
    Template index;

    @Inject
    Template vacationLists;

    @Inject
    WorkflowClient workflowClient;

    @ConfigProperty(name = "quarkus.temporal.worker.task-queue")
    String taskQueue;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        Lists lists = loadLists();
        return index.data("pending", lists.pending()).data("decided", lists.decided());
    }

    @GET
    @Path("/vacations/fragment")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance fragment() {
        Lists lists = loadLists();
        return vacationLists.data("pending", lists.pending()).data("decided", lists.decided());
    }

    private Lists loadLists() {
        List<PendingVacationView> pending = new ArrayList<>();
        List<DecidedVacationView> decided = new ArrayList<>();

        workflowClient.listExecutions("WorkflowType='VacationApprovalWorkflow'")
                .forEach(execution -> {
                    String workflowId = execution.getExecution().getWorkflowId();
                    try {
                        if (execution.getStatus() == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING) {
                            VacationApprovalWorkflow stub = workflowClient.newWorkflowStub(VacationApprovalWorkflow.class, workflowId);
                            VacationSnapshot snapshot = stub.getSnapshot();
                            if (snapshot.request() != null) {
                                pending.add(new PendingVacationView(workflowId, snapshot.request(), snapshot.conflicts(), snapshot.aiSummary(), execution.getStartTime()));
                            }
                        } else if (execution.getStatus() == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED) {
                            WorkflowStub untyped = workflowClient.newUntypedWorkflowStub(workflowId);
                            VacationDecision decision = untyped.getResult(VacationDecision.class);
                            VacationApprovalWorkflow stub = workflowClient.newWorkflowStub(VacationApprovalWorkflow.class, workflowId);
                            VacationSnapshot snapshot = stub.getSnapshot();
                            decided.add(new DecidedVacationView(workflowId, snapshot.request(), decision, execution.getStartTime()));
                        }
                    } catch (Exception e) {
                        // A workflow left over from a previous workflow-code version may no longer
                        // be replayable; skip it rather than breaking the whole list. No stack
                        // trace: this happens on every list load for the same stale executions,
                        // and the cause is always the same replay mismatch.
                        LOG.warnf("Skipping workflow %s while loading the vacation lists: %s", workflowId, e);
                    }
                });

        pending.sort(Comparator.comparing(PendingVacationView::startTime).reversed());
        decided.sort(Comparator.comparing(DecidedVacationView::startTime).reversed());

        return new Lists(pending, decided);
    }

    private record Lists(List<PendingVacationView> pending, List<DecidedVacationView> decided) {
    }

    @POST
    @Path("/vacations")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response submit(@RestForm String employeeName,
                            @RestForm LocalDate startDate,
                            @RestForm LocalDate endDate,
                            @RestForm String reason) {
        VacationRequest request = new VacationRequest(employeeName, startDate, endDate, reason);

        VacationApprovalWorkflow workflow = workflowClient.newWorkflowStub(
                VacationApprovalWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(taskQueue)
                        .setWorkflowId("vacation-" + UUID.randomUUID())
                        .build());

        WorkflowClient.start(workflow::run, request);

        return Response.seeOther(URI.create("/")).build();
    }

    @POST
    @Path("/vacations/{workflowId}/decide")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response decide(@PathParam("workflowId") String workflowId,
                            @RestForm boolean approved,
                            @RestForm String comment) {
        VacationApprovalWorkflow stub = workflowClient.newWorkflowStub(VacationApprovalWorkflow.class, workflowId);
        stub.decide(new ApprovalDecision(approved, comment));
        return Response.seeOther(URI.create("/")).build();
    }
}
