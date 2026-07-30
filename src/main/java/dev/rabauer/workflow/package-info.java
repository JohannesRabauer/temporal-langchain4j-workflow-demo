/**
 * The vacation approval process, now a durable Temporal workflow ({@link
 * dev.rabauer.workflow.VacationApprovalWorkflow}): the conflict check and both AI calls run as
 * retryable {@link dev.rabauer.workflow.VacationActivities}, and the wait for a manager's
 * decision is a {@code @SignalMethod} instead of an in-memory map, so it survives worker
 * restarts, crashes, and redeploys.
 */
package dev.rabauer.workflow;
