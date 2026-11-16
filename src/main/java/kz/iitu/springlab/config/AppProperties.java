package kz.iitu.springlab.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/** Laboratory work no. 3: typed configuration with validation. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String owner,
        @NotBlank String group,
        @Valid Mail mail,
        @Valid Export export) {

    public record Mail(
            @NotBlank @Email String from,
            @Min(1) @Max(10) @DefaultValue("3") int retryCount,
            @DefaultValue("5s") Duration timeout,
            @DefaultValue("true") boolean enabled) {
    }

    /**
     * Individual assignment, variant 8 - prefix app.export:
     * formats (List, @NotEmpty), max-rows @Min(1) @Max(100000).
     */
    public record Export(
            @NotEmpty List<@NotBlank String> formats,
            @Min(1) @Max(100000) @DefaultValue("1000") int maxRows,
            @DefaultValue("true") boolean includeHeader) {
    }
}
