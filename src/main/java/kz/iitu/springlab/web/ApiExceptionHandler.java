package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.BookNotFoundException;
import kz.iitu.springlab.catalog.DuplicateTitleException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * The ONE place where exceptions of the REST controllers become responses.
 * <p>
 * Scoped with annotations = RestController.class, so the HTML page controllers
 * of laboratory work no. 6 are not touched (they have PageExceptionHandler).
 * <p>
 * Extending ResponseEntityExceptionHandler gives problem details
 * (application/problem+json) for the standard Spring MVC failures for free:
 * unreadable JSON, wrong method, missing parameter, failed validation, ...
 */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public ProblemDetail handleNotFound(BookNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());                  // 404
        problem.setType(URI.create("https://api.iitu.kz/errors/book-not-found"));
        problem.setTitle("Book not found");
        return problem;
    }

    @ExceptionHandler(DuplicateTitleException.class)
    public ProblemDetail handleDuplicate(DuplicateTitleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());                   // 409
        problem.setType(URI.create("https://api.iitu.kz/errors/duplicate-title"));
        problem.setTitle("Duplicate title");
        // NB: the extra field must not be called "title" - that name is a standard field
        problem.setProperty("bookTitle", ex.getTitle());
        problem.setProperty("author", ex.getAuthor());
        return problem;
    }

    /**
     * The 400 for a failed @Valid is still produced by the base class
     * (status, type, title, detail, instance). We only add an "errors" list,
     * so the client can see WHICH field was rejected and why.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        ResponseEntity<Object> response =
                super.handleMethodArgumentNotValid(ex, headers, status, request);

        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                    .map(e -> Map.of(
                            "field", e.getField(),
                            "message", String.valueOf(e.getDefaultMessage())))
                    .toList();
            problem.setProperty("errors", errors);
        }
        return response;
    }
}
