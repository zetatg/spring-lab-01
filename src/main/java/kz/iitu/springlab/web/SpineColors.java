package kz.iitu.springlab.web;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Picks the cloth colour of a book's spine (CSS class spine-0 ... spine-4).
 * The colour depends only on the author's name, so one author's books look alike.
 * The form page repeats the same rule in JavaScript for its live preview.
 */
@Component("spineColors")
public class SpineColors {

    public static final int COUNT = 5;

    public int of(String authorName) {
        if (authorName == null || authorName.isBlank()) {
            return 0;
        }
        return Math.floorMod(authorName.strip().toLowerCase(Locale.ROOT).hashCode(), COUNT);
    }
}
