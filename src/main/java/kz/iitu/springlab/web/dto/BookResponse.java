package kz.iitu.springlab.web.dto;

import kz.iitu.springlab.catalog.Genre;

import java.time.Instant;
import java.time.LocalDate;

/** What the contract PROMISES to the client. */
public record BookResponse(
        Long id,
        String title,
        String author,
        int year,
        Genre genre,
        String publisher,     // lab 8 variant 1
        String isbn,          // lab 6 variant 1
        LocalDate publishedOn,  // lab 6 variant 7
        Instant createdAt) {
}
