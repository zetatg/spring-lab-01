package kz.iitu.springlab.lab11;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lab 11 experiments over HTTP. All tokens are SYNTHETIC, signed by DemoAuthorizationServer.
 * Every endpoint is GET, so no CSRF token is needed; /api/lab11/** does its own bearer checks
 * (see SecurityConfiguration).
 */
@RestController
@RequestMapping("/api/lab11")
public class Lab11Controller {

    private final DemoAuthorizationServer issuer;
    private final TokenValidator validator;
    private final InvoiceAccessService invoices;

    public Lab11Controller(DemoAuthorizationServer issuer, TokenValidator validator, InvoiceAccessService invoices) {
        this.issuer = issuer;
        this.validator = validator;
        this.invoices = invoices;
    }

    /** Task A: the handout sample, decoded. */
    @GetMapping("/sample")
    public Map<String, Object> sample() {
        String token = DemoAuthorizationServer.handoutSample();
        DecodedJwt jwt = JwtCodec.decode(token);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", token);
        out.put("header", jwt.header());
        out.put("payload", jwt.claims());
        out.put("signed", jwt.isSigned());
        out.put("issuer", jwt.issuer());
        out.put("subject", jwt.subject());
        out.put("audience", jwt.audience());
        out.put("scopes", jwt.scopes());
        out.put("roles (RBAC)", jwt.roles());
        out.put("department (ABAC attribute)", jwt.attribute("department"));
        out.put("issuedAt", Instant.ofEpochSecond(jwt.issuedAt()).toString());
        out.put("expiresAt", Instant.ofEpochSecond(jwt.expiresAt()).toString());
        out.put("validation", validator.validate(token).failures());
        return out;
    }

    /** A synthetic token of the given kind, e.g. /api/lab11/tokens/VALID or /tokens/EXPIRED. */
    @GetMapping("/tokens/{kind}")
    public Map<String, String> token(@PathVariable DemoAuthorizationServer.Kind kind) {
        return Map.of("kind", kind.name(), "token", issuer.issue(kind));
    }

    /** Task B: validate the bearer token in the Authorization header. */
    @GetMapping("/validate")
    public Map<String, Object> validate(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : "";
        ValidationResult r = validator.validate(token);
        return Map.of("valid", r.valid(), "failedChecks", r.failures());
    }

    /** Task C: a fresh code_verifier and its S256 challenge, plus the flow order. */
    @GetMapping("/pkce")
    public Map<String, Object> pkce() {
        String verifier = Pkce.newVerifier();
        String challenge = Pkce.challengeS256(verifier);
        return Map.of(
                "code_verifier", verifier,
                "code_challenge", challenge,
                "code_challenge_method", "S256",
                "matches", Pkce.matches(verifier, challenge),
                "wrongVerifierMatches", Pkce.matches(Pkce.newVerifier(), challenge),
                "flow", List.of(OAuthFlow.values()),
                "answerForPrintedStatements1to6", OAuthFlow.answerForPrintedStatements());
    }

    /** Task D: /api/lab11/rbac?role=viewer&action=invoices:update */
    @GetMapping("/rbac")
    public Decision rbac(@RequestParam String role, @RequestParam String action) {
        return RbacPolicy.handout().decide(role, action);
    }

    /** Task E: /api/lab11/abac?userDept=finance&invoiceDept=finance&classification=internal&managed=true */
    @GetMapping("/abac")
    public Decision abac(@RequestParam String userDept, @RequestParam String invoiceDept,
                         @RequestParam String classification, @RequestParam boolean managed) {
        return new AbacPolicy().canRead(new AbacPolicy.Subject(userDept),
                new AbacPolicy.Resource(invoiceDept, classification), new AbacPolicy.Environment(managed));
    }

    /** Task F: the protected resource. Header X-Device-Managed simulates the device attribute. */
    @GetMapping("/invoices/{id}")
    public ResponseEntity<Map<String, Object>> invoice(
            @PathVariable long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Device-Managed", defaultValue = "true") boolean deviceManaged) {

        InvoiceAccessService.Result r = invoices.readInvoice(authorization, id, deviceManaged);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("decision", r.decision().label());
        body.put("reasons", r.decision().reasons());
        if (r.invoice() != null) body.put("invoice", r.invoice());

        ResponseEntity.BodyBuilder response = ResponseEntity.status(r.status());
        if (r.status() == 401) {
            response.header("WWW-Authenticate", "Bearer error=\"invalid_token\"");
        } else if (r.status() == 403 && r.decision().reasons().get(0).startsWith("insufficient_scope")) {
            response.header("WWW-Authenticate", "Bearer error=\"insufficient_scope\", scope=\"invoices:read\"");
        }
        return response.body(body);
    }

    /** Task F: the audit log - note that no token is stored in it. */
    @GetMapping("/audit")
    public List<AuditRecord> audit() {
        return invoices.auditLog();
    }
}
