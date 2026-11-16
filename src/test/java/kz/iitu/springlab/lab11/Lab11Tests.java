package kz.iitu.springlab.lab11;

import kz.iitu.springlab.lab11.DemoAuthorizationServer.Kind;
import kz.iitu.springlab.lab11.ValidationResult.Check;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Lab 11: Tasks A-F as executable checks. Plain unit tests - no Spring context needed. */
class Lab11Tests {

    // a fixed "now" for the issued tokens
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T09:00:00Z"), ZoneOffset.UTC);
    private final DemoAuthorizationServer idp = new DemoAuthorizationServer(clock);
    private final TokenValidator validator = new TokenValidator(idp.jwks(),
            DemoAuthorizationServer.ISSUER, DemoAuthorizationServer.API, 60, clock);
    private final InvoiceAccessService api = new InvoiceAccessService(validator,
            RbacPolicy.handout(), new AbacPolicy(), InvoiceAccessService.sampleInvoices(), clock);

    // ---------- Task A ----------

    @Test
    void taskA_sampleHasThreePartsAndReadableClaims() {
        String token = DemoAuthorizationServer.handoutSample();
        assertEquals(3, token.split("\\.", -1).length);           // header.payload.signature

        DecodedJwt jwt = JwtCodec.decode(token);
        assertEquals("RS256", jwt.alg());
        assertEquals("training-key-01", jwt.kid());
        assertEquals("https://identity.example.edu", jwt.issuer());
        assertEquals("user-1042", jwt.subject());
        assertEquals(List.of("https://api.example.edu"), jwt.audience());
        assertEquals(List.of("invoices:read", "profile"), jwt.scopes());
        assertEquals(List.of("finance-analyst"), jwt.roles());      // role
        assertEquals("finance", jwt.attribute("department"));        // attribute
        assertEquals(3600, jwt.expiresAt() - jwt.issuedAt());       // one hour
    }

    @Test
    void taskA_sampleIsNotProofOfAnything() {
        ValidationResult r = validator.validate(DemoAuthorizationServer.handoutSample());
        assertFalse(r.valid());
        assertTrue(r.failed(Check.SIGNATURE));      // unsigned: nothing to verify
        assertTrue(r.failed(Check.EXPIRY));         // and it expired on 10 Nov 2026
        assertTrue(r.failed(Check.TOKEN_PURPOSE));  // typ JWT, not at+jwt
    }

    // ---------- Task B ----------

    @Test
    void taskB_validAccessTokenPassesEveryCheck() {
        assertTrue(validator.validate(idp.issue(Kind.VALID)).valid());
    }

    @Test
    void taskB_eachBadTokenFailsExactlyItsCheck() {
        assertEquals(List.of(Check.SIGNATURE), validator.validate(idp.issue(Kind.FORGED_SIGNATURE)).failures());
        assertEquals(List.of(Check.ISSUER), validator.validate(idp.issue(Kind.WRONG_ISSUER)).failures());
        assertEquals(List.of(Check.AUDIENCE), validator.validate(idp.issue(Kind.WRONG_AUDIENCE)).failures());
        assertEquals(List.of(Check.EXPIRY), validator.validate(idp.issue(Kind.EXPIRED)).failures());
        assertTrue(validator.validate(idp.issue(Kind.ID_TOKEN)).failed(Check.TOKEN_PURPOSE));
        assertTrue(validator.validate(idp.issue(Kind.UNSIGNED)).failed(Check.SIGNATURE));
        assertEquals(List.of(Check.FORMAT), validator.validate("not-a-jwt").failures());
    }

    @Test
    void taskB_decodingSucceedsEvenForAForgedToken() {
        // Base64URL decoding works for anyone's token - authenticity comes only from the signature
        String forged = idp.issue(Kind.FORGED_SIGNATURE);
        assertEquals("user-1042", JwtCodec.decode(forged).subject());
        assertFalse(validator.validate(forged).valid());
    }

    @Test
    void taskB_tamperedPayloadBreaksTheSignature() {
        String[] p = idp.issue(Kind.VALID).split("\\.");
        String evilPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(java.util.Base64.getUrlDecoder().decode(p[1]))
                        .replace("finance-analyst", "billing-admin").getBytes());
        ValidationResult r = validator.validate(p[0] + "." + evilPayload + "." + p[2]);
        assertEquals(List.of(Check.SIGNATURE), r.failures());
    }

    // ---------- Task C ----------

    @Test
    void taskC_flowOrderForTheHandoutStatements() {
        assertEquals(List.of(5, 3, 1, 6, 2, 4), OAuthFlow.answerForPrintedStatements());
    }

    @Test
    void taskC_pkceBindsTheCodeToTheVerifier() {
        String verifier = Pkce.newVerifier();
        String challenge = Pkce.challengeS256(verifier);
        assertTrue(verifier.length() >= 43 && verifier.length() <= 128);
        assertTrue(Pkce.matches(verifier, challenge));
        assertFalse(Pkce.matches(Pkce.newVerifier(), challenge));   // an attacker with only the code
        // RFC 7636 appendix B test vector
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
                Pkce.challengeS256("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
    }

    // ---------- Task D ----------

    @Test
    void taskD_rbacDecisions() {
        RbacPolicy rbac = RbacPolicy.handout();
        assertTrue(rbac.decide("viewer", "invoices:read").allowed());           // Alex
        assertFalse(rbac.decide("viewer", "invoices:update").allowed());        // Alex
        assertTrue(rbac.decide("finance-analyst", "reports:read").allowed());   // Sam
        assertTrue(rbac.decide("billing-admin", "users:manage").allowed());     // Riley
    }

    // ---------- Task E ----------

    @Test
    void taskE_abacDecisions() {
        AbacPolicy abac = new AbacPolicy();
        Decision c1 = abac.canRead(new AbacPolicy.Subject("finance"), new AbacPolicy.Resource("finance", "internal"), new AbacPolicy.Environment(true));
        Decision c2 = abac.canRead(new AbacPolicy.Subject("sales"), new AbacPolicy.Resource("finance", "internal"), new AbacPolicy.Environment(true));
        Decision c3 = abac.canRead(new AbacPolicy.Subject("finance"), new AbacPolicy.Resource("finance", "confidential"), new AbacPolicy.Environment(true));
        Decision c4 = abac.canRead(new AbacPolicy.Subject("finance"), new AbacPolicy.Resource("finance", "public"), new AbacPolicy.Environment(false));

        assertTrue(c1.allowed());
        assertFalse(c2.allowed());
        assertTrue(c2.reasons().get(0).startsWith("department mismatch"));
        assertFalse(c3.allowed());
        assertTrue(c3.reasons().get(0).startsWith("classification"));
        assertFalse(c4.allowed());
        assertEquals(List.of("device is not managed"), c4.reasons());
    }

    // ---------- Task F ----------

    @Test
    void taskF_validTokenReadsOwnDepartmentInvoice() {
        var r = api.readInvoice("Bearer " + idp.issue(Kind.VALID), 42, true);
        assertEquals(200, r.status());
        assertEquals(42, r.invoice().id());
    }

    @Test
    void taskF_negativeCases() {
        assertEquals(401, api.readInvoice(null, 42, true).status());                                    // no token
        assertEquals(401, api.readInvoice("Bearer " + idp.issue(Kind.WRONG_AUDIENCE), 42, true).status());
        assertEquals(401, api.readInvoice("Bearer " + idp.issue(Kind.ID_TOKEN), 42, true).status());
        assertEquals(401, api.readInvoice("Bearer " + idp.issue(Kind.EXPIRED), 42, true).status());
        assertEquals(403, api.readInvoice("Bearer " + idp.issue(Kind.NO_SCOPE), 42, true).status());    // scope
        assertEquals(403, api.readInvoice("Bearer " + idp.issue(Kind.OTHER_DEPARTMENT), 42, true).status());
        assertEquals(403, api.readInvoice("Bearer " + idp.issue(Kind.VALID), 43, true).status());       // confidential
        assertEquals(403, api.readInvoice("Bearer " + idp.issue(Kind.VALID), 45, false).status());      // device
        assertEquals(404, api.readInvoice("Bearer " + idp.issue(Kind.VALID), 999, true).status());
    }

    @Test
    void taskF_auditLogNeverContainsTheToken() {
        String token = idp.issue(Kind.VALID);
        api.readInvoice("Bearer " + token, 42, true);
        api.readInvoice("Bearer " + idp.issue(Kind.OTHER_DEPARTMENT), 42, true);

        assertEquals(2, api.auditLog().size());
        AuditRecord first = api.auditLog().get(0);
        assertEquals("user-1042", first.subject());
        assertEquals("ALLOW", first.decision());
        assertEquals("invoice/42", first.resource());
        assertNotNull(first.tokenId());                         // the jti, for correlation
        assertEquals("DENY", api.auditLog().get(1).decision());
        assertFalse(api.auditLog().toString().contains(token)); // the bearer token itself is never logged
    }
}
