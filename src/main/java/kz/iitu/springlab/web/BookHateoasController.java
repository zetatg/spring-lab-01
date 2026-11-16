package kz.iitu.springlab.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookNotFoundException;
import kz.iitu.springlab.catalog.BookService;
import kz.iitu.springlab.web.dto.BookMapper;
import kz.iitu.springlab.web.dto.BookResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/**
 * Laboratory work no. 7, individual variant 7: HATEOAS.
 * The same book as GET /api/books/{id}, wrapped in an EntityModel with three links:
 * self, books (the collection) and author-books (everything by the same author).
 * The client follows the links instead of building URLs itself.
 */
@Tag(name = "Books (hypermedia)", description = "Lab 7 variant 7")
@RestController
@RequestMapping("/api/hal/books")
public class BookHateoasController {

    private final BookService service;
    private final BookMapper mapper;

    public BookHateoasController(BookService service, BookMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(summary = "One book with hypermedia links (HAL)")
    @GetMapping("/{id}")
    public EntityModel<BookResponse> find(@PathVariable long id) {
        Book book = service.findById(id).orElseThrow(() -> new BookNotFoundException(id));
        BookResponse body = mapper.toResponse(book);

        // linkTo(Class) reads @RequestMapping of the controller, so no URL is written by hand
        Link self = linkTo(BookHateoasController.class).slash(id).withSelfRel();
        Link books = linkTo(BookApiController.class).withRel("books");
        String authorHref = linkTo(BookApiController.class).toUriComponentsBuilder()
                .queryParam("author", body.author())
                .encode().build().toUriString();
        Link authorBooks = Link.of(authorHref, "author-books");

        return EntityModel.of(body, self, books, authorBooks);
    }
}
