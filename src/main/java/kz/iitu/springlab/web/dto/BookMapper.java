package kz.iitu.springlab.web.dto;

import kz.iitu.springlab.catalog.Author;
import kz.iitu.springlab.catalog.Book;
import org.springframework.stereotype.Component;

import java.util.List;

/** The single place that knows both shapes: the entity Book and the DTOs. */
@Component
public class BookMapper {

    public BookResponse toResponse(Book book) {
        Author author = book.getAuthor();
        return new BookResponse(book.getId(), book.getTitle(),
                author != null ? author.getName() : null,
                book.getYear(), book.getGenre(), book.getCreatedAt());
    }

    public List<BookResponse> toResponseList(List<Book> books) {
        return books.stream().map(this::toResponse).toList();
    }
}