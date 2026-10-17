package com.livecomerce.shared.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * During a DB outage every authenticated request waits up to Hikari's connection-timeout for a
 * connection before the auth filter can answer 503. The 30s default would pin a Tomcat thread per
 * request for 30s, so application.yml must keep it short. Binds {@code spring.datasource.hikari}
 * from the real application.yml onto {@link HikariConfig}, the same way Boot does.
 */
class HikariTimeoutsConfigTest {

    @Test
    void defaults_failFastOnConnectionAcquisition_withValidationBelowConnectionTimeout() throws Exception {
        var hikari = bindHikari(Map.of());

        assertThat(hikari.getConnectionTimeout()).isEqualTo(3_000L);
        assertThat(hikari.getValidationTimeout()).isEqualTo(2_000L);
        assertThat(hikari.getValidationTimeout()).isLessThan(hikari.getConnectionTimeout());
    }

    @Test
    void timeouts_canBeOverriddenFromTheEnvironment() throws Exception {
        var hikari = bindHikari(Map.of("DB_CONNECTION_TIMEOUT_MS", "5000", "DB_VALIDATION_TIMEOUT_MS", "4000"));

        assertThat(hikari.getConnectionTimeout()).isEqualTo(5_000L);
        assertThat(hikari.getValidationTimeout()).isEqualTo(4_000L);
    }

    private static HikariConfig bindHikari(Map<String, Object> env) throws Exception {
        var sources = new MutablePropertySources();
        sources.addFirst(new MapPropertySource("env", env));
        new YamlPropertySourceLoader().load("application.yml", new ClassPathResource("application.yml"))
                .forEach(sources::addLast);
        var binder = new Binder(ConfigurationPropertySources.from(sources),
                new PropertySourcesPlaceholdersResolver(sources));
        return binder.bind("spring.datasource.hikari", Bindable.ofInstance(new HikariConfig())).get();
    }
}
