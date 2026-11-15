package kz.iitu.springlab.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** Laboratory work no. 1. */
@RestController
@RequestMapping("/api")
public class HelloController {

    @Value("${app.owner:unknown}")
    private String owner;

    @GetMapping("/hello")
    public Greeting hello(@RequestParam(defaultValue = "world") String name) {
        return new Greeting("Hello, " + name + "!", owner, LocalDateTime.now());
    }

    @GetMapping("/info")
    public Info info() {
        return new Info(owner,
                System.getProperty("java.version"),
                Runtime.getRuntime().availableProcessors());
    }

    /** Individual assignment, variant 8: upper case, lower case and Title Case. */
    @GetMapping("/case")
    public CaseResult caseConversion(@RequestParam(defaultValue = "Hello World") String text) {
        String lower = text.toLowerCase();
        StringBuilder title = new StringBuilder();
        for (String word : lower.split("\\s+")) {
            if (!word.isEmpty()) {
                title.append(Character.toUpperCase(word.charAt(0)))
                     .append(word.substring(1))
                     .append(' ');
            }
        }
        return new CaseResult(text, text.toUpperCase(), lower, title.toString().trim());
    }

    public record Greeting(String message, String owner, LocalDateTime timestamp) { }

    public record Info(String owner, String javaVersion, int cpuCores) { }

    public record CaseResult(String original, String upper, String lower, String title) { }
}
