package kz.iitu.springlab.lab11;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Task C. PKCE (RFC 7636), method S256.
 * The client keeps code_verifier secret and sends only code_challenge = BASE64URL(SHA-256(verifier)).
 * A stolen authorization code is useless without the verifier.
 */
public final class Pkce {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();

    private Pkce() { }

    /** 32 random bytes -> 43 characters, inside the allowed 43..128. */
    public static String newVerifier() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return ENC.encodeToString(bytes);
    }

    public static String challengeS256(String verifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return ENC.encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** What the token endpoint does when the code is exchanged. */
    public static boolean matches(String verifier, String challenge) {
        return verifier != null && MessageDigest.isEqual(
                challengeS256(verifier).getBytes(StandardCharsets.US_ASCII),
                challenge.getBytes(StandardCharsets.US_ASCII));
    }
}
