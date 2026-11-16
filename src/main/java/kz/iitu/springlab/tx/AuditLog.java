package kz.iitu.springlab.tx;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected AuditLog() {
    }

    public AuditLog(String message) {
        this.message = message;
    }

    public Long getId() { return id; }
    public String getMessage() { return message; }
}
