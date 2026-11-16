package kz.iitu.springlab.security;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/** A row of app_user (V5__security.sql). The roles live in user_role. */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    /** A BCrypt hash, never the password itself. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    /** USER, ADMIN - stored WITHOUT the ROLE_ prefix; the UserDetailsService adds it. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 20)
    private Set<String> roles = new HashSet<>();

    protected AppUser() { }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
    public Set<String> getRoles() { return roles; }
}
