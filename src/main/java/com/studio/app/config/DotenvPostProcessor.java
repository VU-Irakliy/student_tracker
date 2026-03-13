package com.studio.app.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads a {@code .env} file from the working directory into the Spring
 * Environment so that {@code ${VAR_NAME}} placeholders in
 * {@code application.properties} are resolved.
 *
 * <p>Silently skipped when the file is missing (e.g. in CI/production).
 */
public class DotenvPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        Path dotenv = Paths.get(".env");
        if (!Files.exists(dotenv)) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        try {
            for (String line : Files.readAllLines(dotenv)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    props.put(key, value);
                }
            }
        } catch (IOException e) {
            // .env file unreadable — fall back to defaults
        }

        environment.getPropertySources()
                .addLast(new MapPropertySource("dotenv", props));
    }
}

