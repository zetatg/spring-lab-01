package kz.iitu.springlab.lab11;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Task A. The three parts of a compact JWT: header.payload.signature.
 * Decoding is NOT verification - see TokenValidator.
 */
public record DecodedJwt(Map<String, Object> header,
                         Map<String, Object> claims,
                         String signingInput,
                         byte[] signature) {

    public String alg() { return str(header.get("alg")); }
    public String typ() { return str(header.get("typ")); }
    public String kid() { return str(header.get("kid")); }

    public String issuer()  { return str(claims.get("iss")); }
    public String subject() { return str(claims.get("sub")); }

    /** aud may be a string or an array of strings. */
    public List<String> audience() {
        Object aud = claims.get("aud");
        if (aud == null) return List.of();
        if (aud instanceof Collection<?> c) return c.stream().map(String::valueOf).toList();
        return List.of(aud.toString());
    }

    public Long expiresAt() { return num(claims.get("exp")); }
    public Long issuedAt()  { return num(claims.get("iat")); }
    public Long notBefore() { return num(claims.get("nbf")); }

    /** "invoices:read profile" -> [invoices:read, profile] */
    public List<String> scopes() {
        String scope = str(claims.get("scope"));
        return scope == null || scope.isBlank() ? List.of() : List.of(scope.trim().split("\\s+"));
    }

    /** RBAC input: roles is a claim with a list of role names. */
    public List<String> roles() {
        Object r = claims.get("roles");
        if (r instanceof Collection<?> c) return c.stream().map(String::valueOf).toList();
        return r == null ? List.of() : List.of(r.toString());
    }

    /** ABAC input: any other claim is an attribute of the subject, e.g. department. */
    public String attribute(String name) { return str(claims.get(name)); }

    public boolean isSigned() { return signature.length > 0; }

    private static String str(Object o) { return o == null ? null : o.toString(); }
    private static Long num(Object o) { return o instanceof Number n ? n.longValue() : null; }
}
