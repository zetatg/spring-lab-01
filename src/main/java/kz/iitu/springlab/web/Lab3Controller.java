package kz.iitu.springlab.web;

import kz.iitu.springlab.config.AppProperties;
import kz.iitu.springlab.config.EnvironmentBanner;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Laboratory work no. 3: the current configuration. */
@RestController
@RequestMapping("/api/lab3")
public class Lab3Controller {

    private final AppProperties props;
    private final EnvironmentBanner banner;
    private final Environment environment;

    public Lab3Controller(AppProperties props, EnvironmentBanner banner, Environment environment) {
        this.props = props;
        this.banner = banner;
        this.environment = environment;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        // a LinkedHashMap, not Map.of: Map.of stops at ten pairs and does not keep the order
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("owner", props.owner());
        out.put("group", props.group());
        out.put("mailFrom", props.mail().from());
        out.put("mailRetryCount", props.mail().retryCount());
        out.put("mailTimeout", props.mail().timeout().toString());
        out.put("mailEnabled", props.mail().enabled());
        // variant 8
        out.put("exportFormats", props.export().formats());
        out.put("exportMaxRows", props.export().maxRows());
        out.put("exportIncludeHeader", props.export().includeHeader());
        out.put("serverPort", environment.getProperty("server.port"));
        out.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        out.put("banner", banner.describe());
        return out;
    }
}
