package kz.iitu.springlab.lab11;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;

/** Encoding, decoding and RS256 signing of compact JWTs, with the JDK only. */
public final class JwtCodec {

    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private JwtCodec() { }

    /** Splits and Base64URL-decodes. Anyone can do this - it proves nothing about who made the token. */
    public static DecodedJwt decode(String token) {
        if (token == null) throw new IllegalArgumentException("No token");
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("A compact JWS has exactly 3 parts, found " + parts.length);
        }
        Map<String, Object> header = Json.parseObject(new String(DEC.decode(parts[0]), StandardCharsets.UTF_8));
        Map<String, Object> claims = Json.parseObject(new String(DEC.decode(parts[1]), StandardCharsets.UTF_8));
        byte[] signature = parts[2].isEmpty() ? new byte[0] : DEC.decode(parts[2]);
        return new DecodedJwt(header, claims, parts[0] + "." + parts[1], signature);
    }

    /** An unsigned teaching token, like the handout sample: header.payload. (empty signature) */
    public static String encodeUnsigned(Map<String, Object> header, Map<String, Object> claims) {
        return b64(Json.write(header)) + "." + b64(Json.write(claims)) + ".";
    }

    /** RS256 = RSASSA-PKCS1-v1_5 with SHA-256 over "header.payload". */
    public static String signRs256(Map<String, Object> header, Map<String, Object> claims, PrivateKey key) {
        String input = b64(Json.write(header)) + "." + b64(Json.write(claims));
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(key);
            s.update(input.getBytes(StandardCharsets.US_ASCII));
            return input + "." + ENC.encodeToString(s.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean verifyRs256(DecodedJwt jwt, PublicKey key) {
        if (!jwt.isSigned()) return false;
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initVerify(key);
            s.update(jwt.signingInput().getBytes(StandardCharsets.US_ASCII));
            return s.verify(jwt.signature());
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static String b64(String json) {
        return ENC.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
