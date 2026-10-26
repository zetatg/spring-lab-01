package kz.iitu.springlab.config;

import kz.iitu.springlab.catalog.Author;
import kz.iitu.springlab.catalog.AuthorRepository;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookRepository;
import kz.iitu.springlab.catalog.Genre;
import kz.iitu.springlab.catalog.Tag;
import kz.iitu.springlab.catalog.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AuthorRepository authors;
    private final BookRepository books;
    private final TagRepository tags;

    public DataSeeder(AuthorRepository authors, BookRepository books, TagRepository tags) {
        this.authors = authors;
        this.books = books;
        this.tags = tags;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (books.count() > 0) {
            return;
        }

        Tag javaTag = tags.save(new Tag("java"));
        Tag springTag = tags.save(new Tag("spring"));
        Tag architecture = tags.save(new Tag("architecture"));
        Tag practices = tags.save(new Tag("best-practices"));

        Author bloch = authors.save(new Author("Joshua Bloch", "USA"));
        Author martin = authors.save(new Author("Robert Martin", "USA"));
        Author walls = authors.save(new Author("Craig Walls", "USA"));

        // the first three get ids 1, 2, 3 - as in lab 7
        add(bloch, "Effective Java", 2018, javaTag, practices);
        add(martin, "Clean Code", 2008, practices);
        add(walls, "Spring in Action", 2022, springTag, javaTag);
        add(martin, "Clean Architecture", 2017, architecture);
        add(martin, "The Clean Coder", 2011, practices);
        add(martin, "Agile Software Development", 2002, architecture, practices);
        add(walls, "Spring Boot in Action", 2015, springTag);
        add(bloch, "Java Puzzlers", 2005, javaTag);

        log.info("Seeded {} authors, {} books, {} tags",
                authors.count(), books.count(), tags.count());
    }

    private void add(Author author, String title, int year, Tag... bookTags) {
        Book book = new Book(title, Genre.PROGRAMMING, year);
        for (Tag tag : bookTags) {
            book.addTag(tag);
        }
        author.addBook(book);
        books.save(book);
    }
}