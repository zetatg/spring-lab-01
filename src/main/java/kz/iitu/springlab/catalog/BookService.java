package kz.iitu.springlab.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository repository;
    private final AuthorRepository authors;

    public BookService(BookRepository repository, AuthorRepository authors) {
        this.repository = repository;
        this.authors = authors;
    }

    /** Used by the HTML page (lab 6): the whole list, author already loaded. */
    public List<Book> findAll(String author) {
        if (author == null || author.isBlank()) {
            return repository.findAllWithAuthor();
        }
        return repository.findByAuthorNameIgnoreCaseOrderById(author);
    }

    /** Used by the REST API (lab 8): paged and sorted. */
    public Page<Book> find(Genre genre, String author, Pageable pageable) {
        boolean hasAuthor = author != null && !author.isBlank();

        if (genre != null && hasAuthor) {
            return repository.findByGenreAndAuthorNameIgnoreCase(genre, author, pageable);
        }
        if (genre != null) {
            return repository.findByGenre(genre, pageable);
        }
        if (hasAuthor) {
            return repository.findByAuthorNameIgnoreCase(author, pageable);
        }
        return repository.findAll(pageable);
    }

    public Optional<Book> findById(long id) {
        return repository.findWithAuthorById(id);
    }

    /** @throws DuplicateTitleException if the same title by the same author exists (-> 409) */
    @Transactional
    public Book create(String title, String authorName, int year, Genre genre) {
        ensureUnique(title, authorName, null);

        Author author = findOrCreateAuthor(authorName);
        Book book = new Book(title, genre, year);
        author.addBook(book);                       // both sides in step
        return repository.save(book);
    }

    /**
     * @throws BookNotFoundException    if there is no book with this id (-> 404)
     * @throws DuplicateTitleException  if the new title/author collides with ANOTHER book (-> 409)
     */
    @Transactional
    public Book update(long id, String title, String authorName, int year, Genre genre) {
        Book book = repository.findWithAuthorById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        ensureUnique(title, authorName, id);

        book.setTitle(title);
        book.setYear(year);
        book.setGenre(genre);
        book.setAuthor(findOrCreateAuthor(authorName));
        return book;        // no save(): the entity is managed, the change is written at commit
    }

    /** 404 for the second DELETE of the same id (decision from lab 7). */
    @Transactional
    public void delete(long id) {
        if (!repository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        repository.deleteById(id);
    }

    /** Task 3.2: the bulk update needs a transaction around it. */
    @Transactional
    public int archiveOlderThan(int year) {
        return repository.archiveOlderThan(year);
    }

    private Author findOrCreateAuthor(String name) {
        return authors.findByNameIgnoreCase(name)
                .orElseGet(() -> authors.save(new Author(name, null)));
    }

    private void ensureUnique(String title, String authorName, Long ignoredId) {
        boolean duplicate = repository
                .findByTitleIgnoreCaseAndAuthorNameIgnoreCase(title, authorName).stream()
                .anyMatch(b -> !b.getId().equals(ignoredId));

        if (duplicate) {
            throw new DuplicateTitleException(title, authorName);
        }
    }
}