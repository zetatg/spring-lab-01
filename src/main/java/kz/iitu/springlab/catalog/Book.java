package kz.iitu.springlab.catalog;

import java.time.Instant;

/**
 * The domain object. Clients never see it directly: the web layer
 * converts it to a BookResponse (see web/dto/BookMapper).
 */
public record Book(Long id, String title, String author, int year,
                   Genre genre, Instant createdAt) {
}
