package kz.iitu.springlab.catalog;

/** Thrown when a book with the same title and author already exists (HTTP 409). */
public class DuplicateTitleException extends RuntimeException {

    private final String title;
    private final String author;

    public DuplicateTitleException(String title, String author) {
        super("A book «" + title + "» by " + author + " already exists");
        this.title = title;
        this.author = author;
    }

    public String getTitle() { return title; }

    public String getAuthor() { return author; }
}
