package kz.iitu.springlab.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Value("${app.owner:unknown}")
    private String owner;

    @GetMapping("/api/hello")
    public Greeting hello(
            @RequestParam(defaultValue = "World") String name) {
        return new Greeting("Hello, " + name + "!");
    }

    @GetMapping("/api/info")
    public Info info() {
        return new Info(
                "spring-lab-01",
                owner,
                "Spring Boot"
        );
    }
    // Individual task: convert input text to uppercase, lowercase, and title case
    @GetMapping("/api/case")
    public CaseResult caseConversion(
            @RequestParam(defaultValue = "Hello World") String text) {

        String upper = text.toUpperCase();
        String lower = text.toLowerCase();

        String[] words = lower.split("\\s+");
        StringBuilder titleBuilder = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                titleBuilder.append(
                        Character.toUpperCase(word.charAt(0))
                ).append(word.substring(1)).append(" ");
            }
        }

        String title = titleBuilder.toString().trim();

        return new CaseResult(upper, lower, title);
    }

    public record Greeting(String message) {}

    public record Info(
            String application,
            String owner,
            String framework
    ) {}

    public record CaseResult(
            String upper,
            String lower,
            String title
    ) {}
}