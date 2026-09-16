package com.ddd.start;

import me.paulschwarz.springdotenv.DotenvConfig;
import me.paulschwarz.springdotenv.DotenvPropertySource;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DotenvTest {

    @Test
    void testDotenvPropertiesLoadedIntoEnvironment() {
        StandardEnvironment env = new StandardEnvironment();

        Properties props = new Properties();
        if (!new File(".env").exists() && new File("../.env").exists()) {
            props.setProperty("directory", "..");
        }
        DotenvConfig config = new DotenvConfig(props);
        DotenvPropertySource propertySource = new DotenvPropertySource(config);
        env.getPropertySources().addLast(propertySource);

        // Server
        assertEquals("8080", env.getProperty("SERVER_PORT"));

        // Database
        assertEquals("localhost", env.getProperty("DATABASE_HOST"));
        assertEquals("5432", env.getProperty("DATABASE_PORT"));
        assertEquals("mydb", env.getProperty("DATABASE_NAME"));
        assertEquals("admin", env.getProperty("DATABASE_USERNAME"));
        assertEquals("admin", env.getProperty("DATABASE_PASSWORD"));

        // Docker Compose
        assertEquals("true", env.getProperty("DOCKER_COMPOSE_ENABLED"));
        assertEquals("compose.yaml", env.getProperty("DOCKER_COMPOSE_FILE"));
        assertEquals("start-only", env.getProperty("DOCKER_COMPOSE_LIFECYCLE"));

        // JWT
        assertEquals("jxgEQeXHuPq8VdbyYFNkANdudQ53YUn4", env.getProperty("JWT_SECRET"));
        assertEquals("86400000", env.getProperty("JWT_EXPIRATION_MS"));
        assertEquals("ddd_jwt", env.getProperty("JWT_COOKIE_NAME"));
        assertEquals("Authorization", env.getProperty("JWT_HEADER"));

        // Logging & JPA
        assertEquals("./logs", env.getProperty("LOG_PATH"));
        assertEquals("true", env.getProperty("SHOW_SQL"));
    }
}
