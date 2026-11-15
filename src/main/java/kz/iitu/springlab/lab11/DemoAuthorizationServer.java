package kz.iitu.springlab.lab11;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A stand-in for the identity provider: it owns an RSA key pair and issues SYNTHETIC tokens
 * for the experiments. A real system would use Keycloak, Auth0, Entra ID... - never this class.
 */
public class DemoAuthorizationServer {

    public static final String ISSUER = "https://identity.example.edu";
    public static final String API = "https://api.example.edu";
    public static final String CLIENT_ID = "finance-web";
    public static final String KID = "training-key-01";

    private final KeyPair keys;
    private final KeyPair attackerKeys;   // a key nobody trusts, for the forged-signature case
    private final Clock clock;

    public DemoAuthorizationServer(Clock clock) {
        this.clock = clock;
        this.keys = rsa();
        this.attackerKeys = rsa();
    }

    /** What the issuer publishes in its JWKS: kid -> public key. */
    public Map<String, PublicKey> jwks() {
        return Map.of(KID, keys.getPublic());
    }

    /** The kinds of token used in the experiments (Task B and the negative tests of Task F). */
    public enum Kind { VALID, WRONG_AUDIENCE, WRONG_ISSUER, EXPIRED, NO_SCOPE, ID_TOKEN, FORGED_SIGNATURE, UNSIGNED, OTHER_DEPARTMENT }

    public String issue(Kind kind) {
        long now = clock.instant().getEpochSecond();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "RS256");
        header.put("typ", kind == Kind.ID_TOKEN ? "JWT" : TokenValidator.ACCESS_TOKEN_TYP);
        header.put("kid", KID);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", kind == Kind.WRONG_ISSUER ? "https://evil.example.com" : ISSUER);
        claims.put("sub", "user-1042");
        claims.put("aud", switch (kind) {
            case WRONG_AUDIENCE -> "https://hr.example.edu";
            case ID_TOKEN -> CLIENT_ID;                  // an ID Token is meant for the client
            default -> API;
        });
        claims.put("iat", kind == Kind.EXPIRED ? now - 7200 : now);
        claims.put("exp", kind == Kind.EXPIRED ? now - 3600 : now + 3600);
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("client_id", CLIENT_ID);
        if (kind == Kind.ID_TOKEN) {
            claims.put("nonce", "n-0S6_WzA2Mj");
        } else {
            claims.put("scope", kind == Kind.NO_SCOPE ? "profile" : "invoices:read profile");
        }
        claims.put("roles", List.of("finance-analyst"));
        claims.put("department", kind == Kind.OTHER_DEPARTMENT ? "sales" : "finance");
        claims.put("device_managed", true);

        return switch (kind) {
            case UNSIGNED -> JwtCodec.encodeUnsigned(header, claims);
            case FORGED_SIGNATURE -> JwtCodec.signRs256(header, claims, attackerKeys.getPrivate());
            default -> JwtCodec.signRs256(header, claims, keys.getPrivate());
        };
    }

    /** The exact sample from the handout (Task A): unsigned, for reading only. */
    public static String handoutSample() {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        header.put("kid", KID);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER);
        claims.put("sub", "user-1042");
        claims.put("aud", API);
        claims.put("iat", 1794301200L);
        claims.put("exp", 1794304800L);
        claims.put("scope", "invoices:read profile");
        claims.put("roles", List.of("finance-analyst"));
        claims.put("department", "finance");
        return JwtCodec.encodeUnsigned(header, claims);
    }

    private static KeyPair rsa() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
