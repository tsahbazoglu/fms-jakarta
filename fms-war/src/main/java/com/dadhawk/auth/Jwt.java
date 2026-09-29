package com.dadhawk.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Minimal HS256 JWT (sub = userId, cid = companyId, perms, exp). Set JWT_SECRET in production. */
public final class Jwt {
    private static final byte[] KEY = Db.env("JWT_SECRET", "dev-only-secret-change-me-32-bytes!").getBytes(UTF_8);
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final String HEADER = ENC.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(UTF_8));

    private Jwt() {}

    private static String mac(String data) {
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(KEY, "HmacSHA256"));
            return ENC.encodeToString(m.doFinal(data.getBytes(UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static String sign(Ctx c, long ttlSeconds) {
        var perms = Json.createArrayBuilder();
        c.perms().forEach(perms::add);
        String payload = ENC.encodeToString(Json.createObjectBuilder()
            .add("sub", c.userId()).add("cid", c.companyId()).add("perms", perms)
            .add("exp", Instant.now().getEpochSecond() + ttlSeconds)
            .build().toString().getBytes(UTF_8));
        String unsigned = HEADER + "." + payload;
        return unsigned + "." + mac(unsigned);
    }

    /** Returns the claims, or null if the token is malformed, tampered with, or expired. */
    public static Ctx verify(String token) {
        try {
            String[] p = token.split("\\.");
            if (p.length != 3 || !p[0].equals(HEADER)) return null;
            if (!MessageDigest.isEqual(mac(p[0] + "." + p[1]).getBytes(UTF_8), p[2].getBytes(UTF_8))) return null;
            JsonObject o = Json.createReader(new StringReader(
                new String(Base64.getUrlDecoder().decode(p[1]), UTF_8))).readObject();
            if (o.getJsonNumber("exp").longValue() < Instant.now().getEpochSecond()) return null;
            var perms = new ArrayList<String>();
            o.getJsonArray("perms").getValuesAs(jakarta.json.JsonString.class).forEach(s -> perms.add(s.getString()));
            return new Ctx(o.getString("sub"), o.getString("cid"), perms);
        } catch (Exception e) { return null; }
    }
}
