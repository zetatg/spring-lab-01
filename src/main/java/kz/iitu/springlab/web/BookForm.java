package kz.iitu.springlab.web;

import jakarta.validation.constraints.*;
import kz.iitu.springlab.catalog.Genre;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class BookForm {

    @NotBlank(message = "{book.title.required}")
    @Size(max = 120, message = "{book.title.size}")
    private String title;

    @NotBlank(message = "{book.author.required}")
    @NoDigits
    private String author;

    @Min(value = 1450, message = "{book.year.range}")
    @Max(value = 2100, message = "{book.year.range}")
    private int year;

    @NotNull(message = "{book.genre.required}")
    private Genre genre;

    /**
     * Lab 6 variant 7. style = "S-": the SHORT date style of the current locale, so the same
     * field is typed and shown as 1/6/18 in English, 06.01.2018 in Russian, 06.01.18 in Kazakh.
     */
    @PastOrPresent(message = "{book.published.past}")
    @DateTimeFormat(style = "S-")
    private LocalDate publishedOn;

    @Isbn                       // lab 6 variant 1: optional, but a filled-in value must be a real ISBN
    private String isbn;

    private boolean available;

    // getters and setters for every field
    // IntelliJ: Alt+Insert → Getters and Setters → select all

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }

    public LocalDate getPublishedOn() { return publishedOn; }
    public void setPublishedOn(LocalDate publishedOn) { this.publishedOn = publishedOn; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}