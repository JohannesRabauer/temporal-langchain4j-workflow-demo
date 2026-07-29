/**
 * The vacation approval process, implemented as plain in-memory Java for now: no persistence,
 * no retries, no recovery after a restart. This is the deliberate starting point for the live
 * session, where these same classes grow into a durable Temporal workflow.
 */
package dev.rabauer.workflow;
