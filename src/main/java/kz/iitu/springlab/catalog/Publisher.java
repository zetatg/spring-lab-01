package kz.iitu.springlab.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Laboratory work no. 8, individual variant 1: a Publisher entity.
 * One publisher, many books. The foreign key book.publisher_id lives on the book side;
 * this side only mirrors it (mappedBy).
 */
@Entity
@Table(name = "publisher")
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 60)
    private String city;

    @OneToMany(mappedBy = "publisher")
    private List<Book> books = new ArrayList<>();

    protected Publisher() { }

    public Publisher(String name, String city) {
        this.name = name;
        this.city = city;
    }

    /** Keeps both sides in step, like Author.addBook. */
    public void addBook(Book book) {
        books.add(book);
        book.setPublisher(this);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public List<Book> getBooks() { return books; }
}
