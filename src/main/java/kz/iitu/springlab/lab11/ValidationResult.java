package kz.iitu.springlab.lab11;

import java.util.List;

/** Task B. Every check that FAILED, in the order they were run. Valid = no failures. */
public record ValidationResult(DecodedJwt jwt, List<Check> failures) {

    public enum Check { FORMAT, ALGORITHM, SIGNATURE, ISSUER, AUDIENCE, EXPIRY, NOT_BEFORE, TOKEN_PURPOSE }

    public boolean valid() { return failures.isEmpty(); }

    public boolean failed(Check check) { return failures.contains(check); }
}
