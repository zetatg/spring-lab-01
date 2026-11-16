package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookService;
import kz.iitu.springlab.catalog.DuplicateTitleException;
import kz.iitu.springlab.catalog.Genre;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequestMapping("/books")
public class BookPageController {

    private final BookService service;

    public BookPageController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Genre genre,
            Model model) {

        List<Book> books = service.findAll(author);
        if (genre != null) {
            books = books.stream().filter(b -> b.getGenre() == genre).toList();
        }

        model.addAttribute("books", books);
        model.addAttribute("author", author);
        model.addAttribute("genre", genre);
        model.addAttribute("genres", Genre.values());

        return "books/list";
    }

    /**
     * Lab 6 variant 7: the date pattern of the current language, shown as the placeholder of the
     * publication date field (en: M/d/yy, ru: dd.MM.y, kk: dd.MM.yy).
     */
    @ModelAttribute("datePattern")
    public String datePattern(Locale locale) {
        return DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale);
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new BookForm());
        model.addAttribute("genres", Genre.values());
        return "books/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BookForm form,
                         BindingResult binding,          // immediately after the form!
                         Model model,
                         RedirectAttributes redirect) {

        if (binding.hasErrors()) {
            model.addAttribute("genres", Genre.values());   // refill the model
            return "books/form";
        }

        Book saved;
        try {
            saved = service.create(form.getTitle(), form.getAuthor(),
                    form.getYear(), form.getGenre(), form.getIsbn(), form.getPublishedOn());
        } catch (DuplicateTitleException ex) {
            // the page has no problem details: show the conflict as a form error
            binding.rejectValue("title", "book.duplicate", ex.getMessage());
            model.addAttribute("genres", Genre.values());
            return "books/form";
        }

        // only the title travels; the sentence itself comes from messages*.properties (EN/RU)
        redirect.addFlashAttribute("addedTitle", saved.getTitle());
        return "redirect:/books";
    }
}