package kz.iitu.springlab.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.iitu.springlab.catalog.Genre;
import kz.iitu.springlab.web.NoDigits;

/**
 * What a client is allowed to SEND: no id and no createdAt, those belong to the server.
 * The validation rules from laboratory work no. 6 are kept.
 */
public record BookRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @NoDigits String author,
        @Min(1450) @Max(2100) int year,
        @NotNull Genre genre) {
}
