package kz.iitu.springlab.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

/**
 * Lab 10. Rules are checked top to bottom and the FIRST match wins,
 * so the specific patterns come first and denyAll() closes everything forgotten.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity            // makes @PreAuthorize on BookService work
public class SecurityConfiguration {

    /** Variant 7: remember-me tokens live in persistent_logins (V6__remember_me.sql). */
    @Bean
    PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    PersistentTokenRepository tokens,
                                    UserDetailsService users) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // internal forwards (the 403 page, /error) must not be blocked a second time
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

                // anonymous
                .requestMatchers("/", "/login", "/403", "/error", "/css/**", "/favicon.ico").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // labs 1-4: read-only demonstration endpoints stay public
                .requestMatchers(HttpMethod.GET, "/api/hello", "/api/info", "/api/case").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/lab2/**", "/api/lab3/**", "/api/lab4/**").permitAll()

                // lab 11: synthetic token experiments; the controller validates its own Bearer tokens
                .requestMatchers(HttpMethod.GET, "/api/lab11/**").permitAll()

                // user and admin may read; only admin may change (create, edit, delete)
                .requestMatchers(HttpMethod.GET, "/books/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/books/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/**").hasRole("ADMIN")

                // lab 3 actuator: health is public, env/configprops/conditions reveal configuration
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // administrator area and the database console
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/h2-console/**").hasRole("ADMIN")

                // everything nobody thought about is closed, not open
                .anyRequest().denyAll())

            // variant 1: our own login page with error and logout messages
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/books")
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())

            // variant 7: remember-me. The "remember-me" checkbox on the login page sets a cookie
            // (series:token) valid for 14 days; the token row is stored in persistent_logins and
            // replaced on every automatic login, and deleted on logout.
            .rememberMe(remember -> remember
                .tokenRepository(tokens)
                .userDetailsService(users)
                .rememberMeCookieName("remember-me")
                .tokenValiditySeconds(14 * 24 * 60 * 60))

            // curl -u user:user123 localhost:8080/api/books
            .httpBasic(Customizer.withDefaults())

            // variant 3: a 403 page of our own instead of the default one
            .exceptionHandling(ex -> ex.accessDeniedPage("/403"))

            // CSRF stays ON. Only the H2 console (a separate servlet that cannot send
            // our token) is excluded, and it is reachable by ADMIN only.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            // the H2 console draws itself in frames
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /** Answers "is this the right password?". Salted, cost factor 10 ($2a$10$...). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
