package kz.iitu.springlab.lab11;

import java.util.List;

/** An ALLOW/DENY answer together with the reasons that produced it. */
public record Decision(boolean allowed, List<String> reasons) {

    public static Decision allow(String reason) { return new Decision(true, List.of(reason)); }
    public static Decision deny(String reason) { return new Decision(false, List.of(reason)); }
    public static Decision deny(List<String> reasons) { return new Decision(false, List.copyOf(reasons)); }

    public String label() { return allowed ? "ALLOW" : "DENY"; }
}
