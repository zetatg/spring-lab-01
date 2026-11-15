package kz.iitu.springlab.labs;

import kz.iitu.springlab.aspect.CallCounterAspect;
import kz.iitu.springlab.aspect.RateLimitAspect;
import kz.iitu.springlab.aspect.RateLimitExceededException;
import kz.iitu.springlab.config.AppProperties;
import kz.iitu.springlab.service.CatalogService;
import kz.iitu.springlab.web.IsbnValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Regression checks for laboratory works 1-8 and their individual variants.
 * Runs with the dev profile (DataSeeder) on its own in-memory database.
 */
@SpringBootTest
@ActiveProfiles({"test", "dev"})
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:labs;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR,VALUE")
class Labs1to8Tests {

    @Autowired WebApplicationContext context;
    @Autowired AppProperties props;
    @Autowired CatalogService catalog;
    @Autowired CallCounterAspect counter;
    @Autowired RateLimitAspect rateLimit;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------- lab 1 ----------

    @Test
    void lab1_helloAndInfo() throws Exception {
        mvc.perform(get("/api/hello").param("name", "IITU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, IITU!"))
                .andExpect(jsonPath("$.owner").value("Turmakov Akylbek"))
                .andExpect(jsonPath("$.timestamp").exists());
        mvc.perform(get("/api/info"))
                .andExpect(jsonPath("$.javaVersion").exists())
                .andExpect(jsonPath("$.cpuCores").isNumber());
    }

    @Test
    void lab1_variant8_caseOnThreeInputs() throws Exception {
        mvc.perform(get("/api/case").param("text", "hello spring boot"))
                .andExpect(jsonPath("$.upper").value("HELLO SPRING BOOT"))
                .andExpect(jsonPath("$.title").value("Hello Spring Boot"));
        mvc.perform(get("/api/case").param("text", "ALMATY iitu"))
                .andExpect(jsonPath("$.lower").value("almaty iitu"))
                .andExpect(jsonPath("$.title").value("Almaty Iitu"));
        mvc.perform(get("/api/case"))                               // missing parameter -> default
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hello World"));
    }

    // ---------- lab 2 ----------

    @Test
    void lab2_notifiersInOrderAndVariant8Html() throws Exception {
        mvc.perform(get("/api/lab2/notify").param("text", "IITU"))
                .andExpect(jsonPath("$.primary").value("email: IITU"))
                .andExpect(jsonPath("$.all", contains("console: IITU", "email: IITU", "<p>IITU</p>", "noop")));
        mvc.perform(get("/api/lab2/custom").param("text", "<b>x</b>"))
                .andExpect(content().string("<p>&lt;b&gt;x&lt;/b&gt;</p>"));
    }

    // ---------- lab 3 ----------

    @Test
    void lab3_devProfileOverridesAndVariant8Export() throws Exception {
        assertEquals(1, props.mail().retryCount());                // application-dev.yml wins over application.yml
        assertFalse(props.mail().enabled());
        assertEquals(List.of("CSV"), props.export().formats());    // variant 8, overridden in dev
        assertEquals(500, props.export().maxRows());
        assertTrue(props.export().includeHeader());                 // @DefaultValue
        mvc.perform(get("/api/lab3/config"))
                .andExpect(jsonPath("$.banner").value("DEVELOPMENT: the data is test data"))
                .andExpect(jsonPath("$.exportMaxRows").value(500));
    }

    // ---------- lab 4 ----------

    @Test
    void lab4_proxyAndVariant1CallCounter() {
        assertTrue(AopUtils.isCglibProxy(catalog));
        int before = counter.getStatistics().getOrDefault("CatalogService.remove(..)", 0);
        catalog.removeTwiceViaThis(5);     // self-invocation: inner calls bypass the proxy
        assertEquals(before, counter.getStatistics().getOrDefault("CatalogService.remove(..)", 0));
        catalog.removeTwice(5);            // through the self proxy: both inner calls are seen
        assertEquals(before + 2, counter.getStatistics().get("CatalogService.remove(..)"));
    }

    // ---------- labs 5, 7, 8 ----------

    @Test
    @WithMockUser(roles = "USER")
    void lab5_variant1_searchByTitleFragment() throws Exception {
        mvc.perform(get("/api/books/search").param("title", "CLEAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/books/search").param("title", "no such book"))
                .andExpect(status().isOk())                         // 200 with [], not 404
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void lab7_variant1_pageCarriesPageSizeAndTotal() throws Exception {
        mvc.perform(get("/api/books").param("page", "1").param("size", "3"))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(3))
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(8)));   // 8 seeded; other tests may add
    }

    @Test
    @WithMockUser(roles = "USER")
    void lab7_missingBookIsProblemDetail() throws Exception {
        mvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Book not found"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void lab8_variant1_publisherDerivedQuery() throws Exception {
        mvc.perform(get("/api/books/by-publisher").param("name", "prentice hall"))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].publisher").value("Prentice Hall"))
                .andExpect(jsonPath("$[0].year").value(2002));            // ordered by year
    }

    // ---------- lab 6 ----------

    @Test
    void lab6_variant1_isbnChecksum() {
        IsbnValidator v = new IsbnValidator();
        assertTrue(v.isValid(null, null));                    // blank passes
        assertTrue(v.isValid("", null));
        assertTrue(v.isValid("978-0-13-468599-1", null));     // ISBN-13
        assertTrue(v.isValid("0-306-40615-2", null));         // ISBN-10
        assertFalse(v.isValid("978-0-13-468599-2", null));    // wrong check digit
        assertFalse(v.isValid("12345", null));                // wrong length
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void lab6_formRejectsBadIsbnAndAcceptsGoodOne() throws Exception {
        mvc.perform(post("/books").with(csrf())
                        .param("title", "Precisely").param("author", "Sestoft")
                        .param("year", "2016").param("genre", "PROGRAMMING").param("isbn", "978-0-13-468599-2"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "isbn"));
        mvc.perform(post("/books").with(csrf())
                        .param("title", "Precisely").param("author", "Sestoft")
                        .param("year", "2016").param("genre", "PROGRAMMING").param("isbn", "0-306-40615-2"))
                .andExpect(redirectedUrl("/books"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void lab6_threeLanguages() throws Exception {
        mvc.perform(get("/books").param("lang", "kk"))
                .andExpect(content().string(containsString("Кітап каталогы")));
        mvc.perform(get("/books").param("lang", "ru"))
                .andExpect(content().string(containsString("Каталог книг")));
    }

    // ================= individual variant 7 (register no. 19 -> 19 - 12 = 7) =================

    @Test
    void lab4_variant7_rateLimitFiveCallsPerMinute() throws Exception {
        rateLimit.reset();
        for (int i = 0; i < 5; i++) {
            catalog.findById(i + 1);                                  // five calls pass
        }
        assertThrows(RateLimitExceededException.class, () -> catalog.findById(6));
        mvc.perform(get("/api/lab4/item/7"))                          // the HTTP face of the same refusal
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Too many requests"));
        rateLimit.reset();
        mvc.perform(get("/api/lab4/item/1")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void lab5_variant7_bulkCreateIsAllOrNothing() throws Exception {
        String two = """
                [{"title":"Refactoring","author":"Martin Fowler","year":2018,"genre":"PROGRAMMING"},
                 {"title":"Domain-Driven Design","author":"Eric Evans","year":2003,"genre":"PROGRAMMING"}]""";
        mvc.perform(post("/api/books/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(two))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").isNumber());

        String withDuplicate = """
                [{"title":"Patterns","author":"Martin Fowler","year":2002,"genre":"PROGRAMMING"},
                 {"title":"Effective Java","author":"Joshua Bloch","year":2018,"genre":"PROGRAMMING"}]""";
        mvc.perform(post("/api/books/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(withDuplicate))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/books/search").param("title", "Patterns"))  // rolled back: the first one is not there
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(post("/api/books/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"title\":\"\",\"author\":\"A\",\"year\":2000,\"genre\":\"FICTION\"}]"))
                .andExpect(status().isBadRequest());                   // validation of every element
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void lab6_variant7_dateFormatDependsOnLocale() throws Exception {
        // Russian short style: dd.MM.y
        mvc.perform(post("/books").with(csrf()).param("lang", "ru")
                        .param("title", "Date RU").param("author", "Tester").param("year", "2018")
                        .param("genre", "FICTION").param("publishedOn", "06.01.2018"))
                .andExpect(redirectedUrl("/books"));
        // English short style: M/d/yy - the Russian spelling is now a type mismatch
        mvc.perform(post("/books").with(csrf()).param("lang", "en")
                        .param("title", "Date EN").param("author", "Tester").param("year", "2018")
                        .param("genre", "FICTION").param("publishedOn", "06.01.2018"))
                .andExpect(model().attributeHasFieldErrorCode("form", "publishedOn", "typeMismatch"));
        mvc.perform(post("/books").with(csrf()).param("lang", "en")
                        .param("title", "Date EN").param("author", "Tester").param("year", "2018")
                        .param("genre", "FICTION").param("publishedOn", "1/6/18"))
                .andExpect(redirectedUrl("/books"));
        // @PastOrPresent
        mvc.perform(post("/books").with(csrf()).param("lang", "ru")
                        .param("title", "Future").param("author", "Tester").param("year", "2030")
                        .param("genre", "FICTION").param("publishedOn", "01.01.2099"))
                .andExpect(model().attributeHasFieldErrorCode("form", "publishedOn", "PastOrPresent"));
        mvc.perform(get("/api/books/search").param("title", "Date RU"))
                .andExpect(jsonPath("$[0].publishedOn").value("2018-01-06"));
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void lab6_variant7_formShowsPatternPerLanguage() throws Exception {
        mvc.perform(get("/books/new").param("lang", "en")).andExpect(content().string(containsString("placeholder=\"M/d/yy\"")));
        mvc.perform(get("/books/new").param("lang", "ru")).andExpect(content().string(containsString("placeholder=\"dd.MM.y\"")));
        mvc.perform(get("/books/new").param("lang", "kk")).andExpect(content().string(containsString("placeholder=\"dd.MM.yy\"")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void lab8_variant7_keysetScrolling() throws Exception {
        mvc.perform(get("/api/books/scroll"))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextAfter").value(3));
        mvc.perform(get("/api/books/scroll").param("after", "3"))
                .andExpect(jsonPath("$.content[0].id").value(4))
                .andExpect(jsonPath("$.nextAfter").value(6));
    }
}
