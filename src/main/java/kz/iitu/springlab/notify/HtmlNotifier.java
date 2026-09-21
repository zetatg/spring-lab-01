package kz.iitu.springlab.notify;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("html")
@Order(3)
public class HtmlNotifier implements Notifier {

    private static final Logger log =
            LoggerFactory.getLogger(HtmlNotifier.class);

    @PostConstruct
    void init() {
        log.info("HTML NOTIFIER >> initialized");
    }

    @Override
    public String send(String message) {
        String escaped = message
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        return "<p>" + escaped + "</p>";
    }

    @Override
    public String channel() {
        return "html";
    }
}