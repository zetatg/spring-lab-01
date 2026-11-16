package kz.iitu.springlab.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    // ---- Task 3.1 / Listing 4: five derived queries
    List<Book> findByGenre(Genre genre);

    List<Book> findByTitleContainingIgnoreCase(String fragment);

    List<Book> findByYearBetweenOrderByYearDesc(int from, int to);

    List<Book> findTop3ByGenreOrderByYearDesc(Genre genre);

    long countByAuthorName(String name);        // a nested property

    // ---- Task 3.2 / Listing 5: JPQL and a bulk update
    @Query("""
           select b from Book b
           where b.genre = :genre and b.year >= :year
           order by b.title""")
    List<Book> search(@Param("genre") Genre genre, @Param("year") int year);

    @Modifying(clearAutomatically = true)
    @Query("update Book b set b.archived = true where b.year < :year")
    int archiveOlderThan(@Param("year") int year);     // @Transactional on the caller

    // ---- Task 3.4 / Listing 6: the cure for N+1
    @Query("select b from Book b join fetch b.author left join fetch b.publisher order by b.id")
    List<Book> findAllWithAuthor();

    // ---- Task 4 / Listing 7: paged versions (author loaded, see the note at the top)
    @EntityGraph(attributePaths = {"author", "publisher"})
    Page<Book> findByGenre(Genre genre, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "publisher"})
    Page<Book> findByAuthorNameIgnoreCase(String author, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "publisher"})
    Page<Book> findByGenreAndAuthorNameIgnoreCase(Genre genre, String author, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author", "publisher"})
    Page<Book> findAll(Pageable pageable);

    // Task 4.3: the Slice variant, used by Lab8Demo so you do not have to edit the controller
    @EntityGraph(attributePaths = {"author", "publisher"})
    Slice<Book> findSliceByGenre(Genre genre, Pageable pageable);

    // ---- helpers for BookService
    @EntityGraph(attributePaths = {"author", "publisher"})
    Optional<Book> findWithAuthorById(Long id);

    @EntityGraph(attributePaths = {"author", "publisher"})
    List<Book> findByAuthorNameIgnoreCaseOrderById(String author);

    List<Book> findByTitleIgnoreCaseAndAuthorNameIgnoreCase(String title, String author);

    // ---- lab 5 variant 1: search by a fragment of the title, case-insensitive
    @EntityGraph(attributePaths = {"author", "publisher"})
    List<Book> findByTitleContainingIgnoreCaseOrderByTitle(String fragment);

    // ---- lab 8 variant 7: keyset scrolling. "where id > ? order by id fetch first ? rows"
    // instead of "offset ? rows" - the database seeks to the key instead of skipping rows.
    @EntityGraph(attributePaths = {"author", "publisher"})
    Window<Book> findFirst3ByOrderByIdAsc(ScrollPosition position);

    // ---- lab 8 variant 1: a derived query through the association - joins book.publisher_id
    @EntityGraph(attributePaths = {"author", "publisher"})
    List<Book> findByPublisherNameIgnoreCaseOrderByYear(String publisherName);
}