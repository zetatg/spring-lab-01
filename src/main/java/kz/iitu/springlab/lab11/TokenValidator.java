package kz.iitu.springlab.lab11;

import kz.iitu.springlab.lab11.ValidationResult.Check;

import java.security.PublicKey;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Task B. What a resource server must check before it trusts an access token.
 * Scope is checked separately (ScopeCheck / InvoiceAccessService): a valid token
 * without the right scope is a 403, not a 401.
 */
public class TokenValidator {

    /** Header typ of an access token (RFC 9068). An ID Token has typ "JWT" and is meant for the client. */
    public static final String ACCESS_TOKEN_TYP = "at+jwt";

    private final Map<String, PublicKey> trustedKeys;   // kid -> key, as published in the issuer's JWKS
    private final String expectedIssuer;
    private final String expectedAudience;
    private final long clockSkewSeconds;
    private final Clock clock;

    public TokenValidator(Map<String, PublicKey> trustedKeys, String expectedIssuer,
                          String expectedAudience, long clockSkewSeconds, Clock clock) {
        this.trustedKeys = Map.copyOf(trustedKeys);
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
        this.clockSkewSeconds = clockSkewSeconds;
        this.clock = clock;
    }

    public ValidationResult validate(String token) {
        DecodedJwt jwt;
        try {
            jwt = JwtCodec.decode(token);
        } catch (RuntimeException e) {
            return new ValidationResult(null, List.of(Check.FORMAT));
        }

        List<Check> failures = new ArrayList<>();
        long now = clock.instant().getEpochSecond();

        // only the algorithm we expect: never "none", never HS256 with a public key
        if (!"RS256".equals(jwt.alg())) failures.add(Check.ALGORITHM);

        PublicKey key = jwt.kid() == null ? null : trustedKeys.get(jwt.kid());
        if (key == null || !JwtCodec.verifyRs256(jwt, key)) failures.add(Check.SIGNATURE);

        if (!expectedIssuer.equals(jwt.issuer())) failures.add(Check.ISSUER);
        if (!jwt.audience().contains(expectedAudience)) failures.add(Check.AUDIENCE);

        Long exp = jwt.expiresAt();
        if (exp == null || now > exp + clockSkewSeconds) failures.add(Check.EXPIRY);
        Long nbf = jwt.notBefore();
        if (nbf != null && now + clockSkewSeconds < nbf) failures.add(Check.NOT_BEFORE);

        if (!ACCESS_TOKEN_TYP.equalsIgnoreCase(jwt.typ())) failures.add(Check.TOKEN_PURPOSE);

        return new ValidationResult(jwt, List.copyOf(failures));
    }
}
