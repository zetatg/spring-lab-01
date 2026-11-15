package kz.iitu.springlab.lab11;

import java.time.Instant;

/**
 * Task F. What is written for every access decision. The raw bearer token is NEVER stored:
 * anyone reading the log could replay it until it expires. Only the jti is kept, for correlation.
 */
public record AuditRecord(Instant time, String subject, String clientId, String tokenId,
                          String action, String resource, String decision, String reason,
                          String policyVersion) { }
