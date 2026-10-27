package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.BookService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final BookService service;

    public HomeController(BookService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String home(Model model) {
        // the five newest books; BookService.find loads the author with the book
        model.addAttribute("recent", service
                .find(null, null, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id")))
                .getContent());
        return "home";
    }
}
