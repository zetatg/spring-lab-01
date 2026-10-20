package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.BookNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Scoped to the page controller ONLY.
 * <p>
 * Why not annotations = Controller.class as before? @RestController is itself
 * meta-annotated with @Controller, so that selector also matched the REST
 * controllers, and two advices would compete for BookNotFoundException
 * (the winner would depend on bean ordering). assignableTypes removes the overlap.
 */
@ControllerAdvice(assignableTypes = BookPageController.class)
public class PageExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(BookNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/not-found";
    }
}
