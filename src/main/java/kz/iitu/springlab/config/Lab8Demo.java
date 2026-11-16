package kz.iitu.springlab.config;

import jakarta.persistence.EntityManagerFactory;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookRepository;
import kz.iitu.springlab.catalog.BookService;
import kz.iitu.springlab.catalog.Genre;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Supplier;

@Component
@Profile("demo")
@Order(2)
public class Lab8Demo implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Lab8Demo.class);

    private final BookRepository books;
    private final BookService service;
    private final TransactionTemplate tx;
    private final Statistics stats;

    public Lab8Demo(BookRepository books, BookService service,
                    PlatformTransactionManager transactionManager,
                    EntityManagerFactory emf) {
        this.books = books;
        this.service = service;
        this.tx = new TransactionTemplate(transactionManager);
        this.stats = emf.unwrap(SessionFactory.class).getStatistics();
    }

    @Override
    public void run(String... args) {
        // Task 3.3
        measure("findByGenre(PROGRAMMING)",
                () -> books.findByGenre(Genre.PROGRAMMING).size());
        measure("findByTitleContainingIgnoreCase(\"clean\")",
                () -> books.findByTitleContainingIgnoreCase("clean").size());
        measure("findByYearBetweenOrderByYearDesc(2005, 2015)",
                () -> books.findByYearBetweenOrderByYearDesc(2005, 2015).size());
        measure("findTop3ByGenreOrderByYearDesc(PROGRAMMING)",
                () -> books.findTop3ByGenreOrderByYearDesc(Genre.PROGRAMMING).size());
        measure("countByAuthorName(\"Robert Martin\")",
                () -> books.countByAuthorName("Robert Martin"));
        measure("search(PROGRAMMING, 2010)  [JPQL]",
                () -> books.search(Genre.PROGRAMMING, 2010).size());

        // Task 3.4: N+1, before and after
        measure("N+1 BEFORE: findAll() + getAuthor().getName()",
                () -> printAuthors(books.findAll()));
        measure("N+1 AFTER:  findAllWithAuthor() [join fetch]",
                () -> printAuthors(books.findAllWithAuthor()));

        // Task 4.3: Page (data + count) against Slice (data only)
        measure("PAGE  findByGenre(PROGRAMMING, page 0, size 3)", () -> {
            Page<Book> page = books.findByGenre(Genre.PROGRAMMING,
                    PageRequest.of(0, 3, Sort.by("title")));
            return "rows=" + page.getContent().size() + ", total=" + page.getTotalElements();
        });
        measure("SLICE findSliceByGenre(PROGRAMMING, page 0, size 3)", () -> {
            Slice<Book> slice = books.findSliceByGenre(Genre.PROGRAMMING,
                    PageRequest.of(0, 3, Sort.by("title")));
            return "rows=" + slice.getContent().size() + ", hasNext=" + slice.hasNext();
        });

        // last, because it changes data: Task 3.2 bulk update
        measure("archiveOlderThan(2010)  [bulk update]",
                () -> service.archiveOlderThan(2010));
    }

    private int printAuthors(List<Book> list) {
        list.forEach(b -> log.info("      {} -> {}", b.getTitle(), b.getAuthor().getName()));
        return list.size();
    }

    private void measure(String label, Supplier<Object> action) {
        log.info(">>> {}", label);
        stats.clear();
        Object result = tx.execute(status -> action.get());
        log.info("<<< {} => result = {} | SQL statements = {}",
                label, result, stats.getPrepareStatementCount());
    }
}