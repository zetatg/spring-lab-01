package kz.iitu.springlab.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Laboratory work no. 6, individual variant 1: an ISBN field with a custom constraint.
 * Checks the length and the checksum of an ISBN-10 or ISBN-13. A blank value passes
 * (the field is optional); an invalid one gives its own message.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsbnValidator.class)
public @interface Isbn {

    String message() default "{book.isbn.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
