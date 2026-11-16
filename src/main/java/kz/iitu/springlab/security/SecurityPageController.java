package kz.iitu.springlab.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class SecurityPageController {

    private final AppUserRepository users;

    public SecurityPageController(AppUserRepository users) {
        this.users = users;
    }

    /** Variant 1: our own login page. POST /login is handled by Spring Security itself. */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Variant 3: the page shown by accessDeniedPage("/403").
     * Any method, not only GET: the handler FORWARDS the refused request, so a refused POST
     * (for example a form sent without its CSRF token) arrives here as a POST.
     */
    @RequestMapping("/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied() {
        return "error/403";
    }

    /** Task 3.1: the administrator area - one page listing the users. */
    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("users", users.findAllByOrderById());
        return "admin/users";
    }
}
