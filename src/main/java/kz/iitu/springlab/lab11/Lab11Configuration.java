package kz.iitu.springlab.lab11;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Lab 11 beans. The classes themselves are plain Java, so they are unit-tested without Spring. */
@Configuration
public class Lab11Configuration {

    @Bean
    Clock lab11Clock() {
        return Clock.systemUTC();
    }

    @Bean
    DemoAuthorizationServer demoAuthorizationServer(Clock lab11Clock) {
        return new DemoAuthorizationServer(lab11Clock);
    }

    @Bean
    TokenValidator tokenValidator(DemoAuthorizationServer issuer, Clock lab11Clock) {
        return new TokenValidator(issuer.jwks(), DemoAuthorizationServer.ISSUER,
                DemoAuthorizationServer.API, 60, lab11Clock);
    }

    @Bean
    InvoiceAccessService invoiceAccessService(TokenValidator validator, Clock lab11Clock) {
        return new InvoiceAccessService(validator, RbacPolicy.handout(), new AbacPolicy(),
                InvoiceAccessService.sampleInvoices(), lab11Clock);
    }
}
