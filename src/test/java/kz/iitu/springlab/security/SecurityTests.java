package kz.iitu.springlab.security;

import kz.iitu.springlab.catalog.BookService;
import kz.iitu.springlab.catalog.Genre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Lab 10, variant 12 (plus the CSRF and BCrypt checks of tasks 2 and 4). */
@SpringBootTest
@ActiveProfiles("test")
class SecurityTests {

    @Autowired WebApplicationContext context;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired BookService books;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------- the three @WithMockUser tests ----------

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/books").accept(MediaType.TEXT_HTML))   // a browser asks for HTML
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void userIsForbiddenFromAdmin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"USER", "ADMIN"})
    void adminIsAllowedIntoAdmin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"));
    }

    // ---------- more of the access matrix ----------

    @Test
    void homeAndLoginAreOpen() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userMayReadBooksButNotCreate() throws Exception {
        mvc.perform(get("/books")).andExpect(status().isOk());
        mvc.perform(post("/books").with(csrf())
                        .param("title", "Forged").param("author", "Nobody")
                        .param("year", "2020").param("genre", "FICTION"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void unknownUrlIsDeniedEvenForAdmin() throws Exception {
        mvc.perform(get("/nowhere")).andExpect(status().isForbidden());
    }

    // ---------- task 4: CSRF ----------

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/books")
                        .param("title", "Forged").param("author", "Nobody")
                        .param("year", "2020").param("genre", "FICTION"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void postWithCsrfTokenIsAccepted() throws Exception {
        mvc.perform(post("/books").with(csrf())
                        .param("title", "Genuine " + System.nanoTime()).param("author", "Somebody")
                        .param("year", "2020").param("genre", "FICTION"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));
    }

    // ---------- task 3.2: the method rule works without any URL ----------

    @Test
    @WithMockUser(roles = "USER")
    void serviceRefusesUserEvenWithoutController() {
        assertThrows(AccessDeniedException.class,
                () -> books.create("Bypass", "Nobody", 2020, Genre.FICTION));
    }

    // ---------- task 2: hashes, not passwords ----------

    @Test
    void passwordsAreStoredAsBcryptHashes() {
        AppUser user = users.findByUsername("user").orElseThrow();
        AppUser admin = users.findByUsername("admin").orElseThrow();

        assertTrue(user.getPasswordHash().startsWith("$2a$10$"));
        assertEquals(60, user.getPasswordHash().length());
        assertNotEquals("user123", user.getPasswordHash());
        assertTrue(encoder.matches("user123", user.getPasswordHash()));
        assertTrue(encoder.matches("admin123", admin.getPasswordHash()));
        assertTrue(admin.getRoles().contains("ADMIN"));

        // same password, two hashes: the salt is random
        assertNotEquals(encoder.encode("user123"), encoder.encode("user123"));
    }

    // ---------- variant 7: remember-me ----------

    @Test
    void rememberMeSetsAFourteenDayCookieAndStoresTheToken() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("username", "user").param("password", "user123").param("remember-me", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(cookie().exists("remember-me"))
                .andExpect(cookie().maxAge("remember-me", 14 * 24 * 60 * 60))
                .andExpect(cookie().httpOnly("remember-me", true));
        assertTrue(jdbc.queryForObject(
                "select count(*) from persistent_logins where username = 'user'", Integer.class) >= 1);
    }

    @Test
    void withoutTheCheckboxThereIsNoRememberMeCookie() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "user").param("password", "user123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(cookie().doesNotExist("remember-me"));
    }
}
