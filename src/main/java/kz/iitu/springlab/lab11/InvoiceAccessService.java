package kz.iitu.springlab.lab11;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Task F. The access decision for GET /invoices/{id}:
 * 1) token validation (401 on failure), 2) scope invoices:read (403),
 * 3) RBAC: a role that has invoices:read (403), 4) ABAC: department, classification, device (403),
 * 5) the invoice must exist (404), and every decision is audited without the token.
 */
public class InvoiceAccessService {

    public static final String ACTION = "invoices:read";
    public static final String POLICY_VERSION = "invoice-read-v1";

    public record Result(int status, Decision decision, Invoice invoice) { }

    private final TokenValidator validator;
    private final RbacPolicy rbac;
    private final AbacPolicy abac;
    private final Map<Long, Invoice> invoices;
    private final Clock clock;
    private final List<AuditRecord> audit = new CopyOnWriteArrayList<>();

    public InvoiceAccessService(TokenValidator validator, RbacPolicy rbac, AbacPolicy abac,
                                Map<Long, Invoice> invoices, Clock clock) {
        this.validator = validator;
        this.rbac = rbac;
        this.abac = abac;
        this.invoices = Map.copyOf(invoices);
        this.clock = clock;
    }

    public static Map<Long, Invoice> sampleInvoices() {
        return Map.of(
                42L, new Invoice(42, "finance", "internal", "Laboratory equipment", "1 250 000 KZT"),
                43L, new Invoice(43, "finance", "confidential", "Salary fund", "48 000 000 KZT"),
                44L, new Invoice(44, "sales", "internal", "Conference booth", "600 000 KZT"),
                45L, new Invoice(45, "finance", "public", "Annual report printing", "90 000 KZT"));
    }

    /**
     * @param authorization the Authorization header value, "Bearer <token>"
     * @param deviceManaged environment attribute; in real life it comes from the device-management system
     */
    public Result readInvoice(String authorization, long id, boolean deviceManaged) {
        String resource = "invoice/" + id;

        // 1. a bearer token must be present and valid
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return finish(401, Decision.deny("no bearer token"), null, null, resource);
        }
        ValidationResult vr = validator.validate(authorization.substring(7).trim());
        if (!vr.valid()) {
            return finish(401, Decision.deny("invalid token: " + vr.failures()), vr.jwt(), null, resource);
        }
        DecodedJwt jwt = vr.jwt();

        // 2. the token must carry the scope for this action
        if (!jwt.scopes().contains(ACTION)) {
            return finish(403, Decision.deny("insufficient_scope: " + ACTION + " missing"), jwt, null, resource);
        }

        // 3. RBAC: one of the roles must grant invoices:read
        Decision byRole = rbac.decide(jwt.roles(), ACTION);
        if (!byRole.allowed()) {
            return finish(403, byRole, jwt, null, resource);
        }

        // 4. the resource; a missing invoice is 404
        Invoice invoice = invoices.get(id);
        if (invoice == null) {
            return finish(404, Decision.deny("no invoice " + id), jwt, null, resource);
        }

        // 5. ABAC on subject, resource and environment attributes
        Decision byAttributes = abac.canRead(
                new AbacPolicy.Subject(jwt.attribute("department")),
                new AbacPolicy.Resource(invoice.department(), invoice.classification()),
                new AbacPolicy.Environment(deviceManaged));
        if (!byAttributes.allowed()) {
            return finish(403, byAttributes, jwt, null, resource);
        }

        List<String> reasons = new ArrayList<>(byRole.reasons());
        reasons.addAll(byAttributes.reasons());
        return finish(200, new Decision(true, reasons), jwt, invoice, resource);
    }

    public List<AuditRecord> auditLog() {
        return Collections.unmodifiableList(audit);
    }

    private Result finish(int status, Decision d, DecodedJwt jwt, Invoice invoice, String resource) {
        audit.add(new AuditRecord(
                clock.instant(),
                jwt == null ? null : jwt.subject(),
                jwt == null ? null : jwt.attribute("client_id"),
                jwt == null ? null : jwt.attribute("jti"),       // the token id, never the token
                ACTION, resource, d.label(), String.join("; ", d.reasons()), POLICY_VERSION));
        return new Result(status, d, invoice);
    }

    public Optional<Invoice> find(long id) { return Optional.ofNullable(invoices.get(id)); }
}
