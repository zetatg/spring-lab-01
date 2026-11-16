package kz.iitu.springlab.config;

import kz.iitu.springlab.catalog.Author;
import kz.iitu.springlab.catalog.AuthorRepository;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookRepository;
import kz.iitu.springlab.catalog.Genre;
import kz.iitu.springlab.catalog.Publisher;
import kz.iitu.springlab.catalog.PublisherRepository;
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
    private final PublisherRepository publishers;

    public DataSeeder(AuthorRepository authors, BookRepository books, TagRepository tags,
                      PublisherRepository publishers) {
        this.publishers = publishers;
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

        // lab 8 variant 1: three publishers; Prentice Hall gets four books
        Publisher addison = publishers.save(new Publisher("Addison-Wesley", "Boston"));
        Publisher prentice = publishers.save(new Publisher("Prentice Hall", "Upper Saddle River"));
        Publisher manning = publishers.save(new Publisher("Manning", "Shelter Island"));

        // the first three get ids 1, 2, 3 - as in lab 7
        add(bloch, addison, "Effective Java", 2018, "978-0-13-468599-1", javaTag, practices);
        add(martin, prentice, "Clean Code", 2008, "978-0-13-235088-4", practices);
        add(walls, manning, "Spring in Action", 2022, "978-1-61729-757-1", springTag, javaTag);
        add(martin, prentice, "Clean Architecture", 2017, "978-0-13-449416-6", architecture);
        add(martin, prentice, "The Clean Coder", 2011, "978-0-13-708107-3", practices);
        add(martin, prentice, "Agile Software Development", 2002, null, architecture, practices);
        add(walls, manning, "Spring Boot in Action", 2015, null, springTag);
        add(bloch, addison, "Java Puzzlers", 2005, "978-0-321-33678-1", javaTag);

        log.info("Seeded {} authors, {} publishers, {} books, {} tags",
                authors.count(), publishers.count(), books.count(), tags.count());
    }

    private void add(Author author, Publisher publisher, String title, int year, String isbn, Tag... bookTags) {
        Book book = new Book(title, Genre.PROGRAMMING, year);
        for (Tag tag : bookTags) {
            book.addTag(tag);
        }
        book.setIsbn(isbn);
        author.addBook(book);
        publisher.addBook(book);
        books.save(book);
    }
}