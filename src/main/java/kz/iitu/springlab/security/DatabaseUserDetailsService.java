package kz.iitu.springlab.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers "who is this?": the username, the stored BCrypt hash and the authorities.
 * Checking the password is NOT done here - that is the PasswordEncoder's job.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public DatabaseUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser u = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return User.withUsername(u.getUsername())
                .password(u.getPasswordHash())
                .authorities(u.getRoles().stream()
                        .map(r -> "ROLE_" + r)            // hasRole("ADMIN") checks ROLE_ADMIN
                        .toArray(String[]::new))
                .disabled(!u.isEnabled())
                .build();
    }
}
