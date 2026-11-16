package kz.iitu.springlab.labs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Laboratory work no. 7, individual variant 7: HATEOAS links on one book. */
@SpringBootTest
@ActiveProfiles({"test", "dev"})
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:hal;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR,VALUE")
class Lab7HateoasTests {

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void bookCarriesSelfBooksAndAuthorLinks() throws Exception {
        mvc.perform(get("/api/hal/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Effective Java"))
                .andExpect(jsonPath("$._links.self.href", endsWith("/api/hal/books/1")))
                .andExpect(jsonPath("$._links.books.href", endsWith("/api/books")))
                .andExpect(jsonPath("$._links.author-books.href", endsWith("/api/books?author=Joshua%20Bloch")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void missingBookIs404() throws Exception {
        mvc.perform(get("/api/hal/books/999")).andExpect(status().isNotFound());
    }
}
