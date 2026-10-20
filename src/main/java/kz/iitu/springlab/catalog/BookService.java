package kz.iitu.springlab.catalog;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public List<Book> findAll(String author) {
        if (author == null) {
            return repository.findAll();
        }

        return repository.findAll().stream()
                .filter(book -> book.author().equalsIgnoreCase(author))
                .toList();
    }

    public Optional<Book> findById(long id) {
        return repository.findById(id);
    }

    /** @throws DuplicateTitleException if the same title by the same author exists (-> 409) */
    public Book create(Book book) {
        ensureUnique(book, null);
        return repository.save(book);
    }

    /**
     * @throws BookNotFoundException    if there is no book with this id (-> 404)
     * @throws DuplicateTitleException  if the new title/author collides with ANOTHER book (-> 409)
     */
    public Book update(long id, Book book) {
        Book existing = repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        ensureUnique(book, id);

        // id and createdAt belong to the server, not to the client
        return repository.save(new Book(id, book.title(), book.author(),
                book.year(), book.genre(), existing.createdAt()));
    }

    /**
     * Design decision for the second DELETE of the same id: 404.
     * The server state after one or ten calls is the same (the book is gone),
     * so DELETE stays idempotent, but the client is told the truth:
     * "there is nothing with this id".
     *
     * @throws BookNotFoundException if there is no book with this id (-> 404)
     */
    public void delete(long id) {
        if (!repository.deleteById(id)) {
            throw new BookNotFoundException(id);
        }
    }

    private void ensureUnique(Book candidate, Long ignoredId) {
        boolean duplicate = repository.findAll().stream()
                .filter(b -> !b.id().equals(ignoredId))
                .anyMatch(b -> b.title().equalsIgnoreCase(candidate.title())
                        && b.author().equalsIgnoreCase(candidate.author()));

        if (duplicate) {
            throw new DuplicateTitleException(candidate.title(), candidate.author());
        }
    }
}
