package kz.iitu.springlab.lab11;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Task E. Attribute-based access control. A user may read an invoice only when ALL are true:
 * subject.department == resource.department; resource.classification is internal or public;
 * environment.deviceManaged is true. Every failed condition is reported, not only the first.
 */
public class AbacPolicy {

    public record Subject(String department) { }
    public record Resource(String department, String classification) { }
    public record Environment(boolean deviceManaged) { }

    private static final Set<String> READABLE = Set.of("internal", "public");

    public Decision canRead(Subject subject, Resource resource, Environment env) {
        List<String> denied = new ArrayList<>();
        if (subject.department() == null || !subject.department().equals(resource.department())) {
            denied.add("department mismatch: " + subject.department() + " != " + resource.department());
        }
        if (!READABLE.contains(resource.classification())) {
            denied.add("classification " + resource.classification() + " is not internal or public");
        }
        if (!env.deviceManaged()) {
            denied.add("device is not managed");
        }
        return denied.isEmpty() ? Decision.allow("all three conditions are true") : Decision.deny(denied);
    }
}
