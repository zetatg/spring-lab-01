package kz.iitu.springlab.lab11;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** Task D. Role-based access control: permissions belong to roles, users get roles. */
public class RbacPolicy {

    private final Map<String, Set<String>> permissionsByRole;

    public RbacPolicy(Map<String, Set<String>> permissionsByRole) {
        this.permissionsByRole = Map.copyOf(permissionsByRole);
    }

    /** The policy from the handout. */
    public static RbacPolicy handout() {
        return new RbacPolicy(Map.of(
                "viewer", Set.of("invoices:read"),
                "finance-analyst", Set.of("invoices:read", "reports:read"),
                "billing-admin", Set.of("invoices:read", "invoices:update", "users:manage")));
    }

    public Decision decide(String role, String action) {
        Set<String> perms = permissionsByRole.get(role);
        if (perms == null) return Decision.deny("unknown role " + role);
        return perms.contains(action)
                ? Decision.allow(role + " has " + action)
                : Decision.deny(role + " has only " + String.join(", ", sorted(perms)) + "; " + action + " is not granted");
    }

    /** A user with several roles is allowed if ANY role grants the action. */
    public Decision decide(Collection<String> roles, String action) {
        for (String role : roles) {
            Decision d = decide(role, action);
            if (d.allowed()) return d;
        }
        return Decision.deny("none of the roles " + roles + " grants " + action);
    }

    private static java.util.List<String> sorted(Set<String> s) {
        return s.stream().sorted().toList();
    }
}
