package com.ddd.infrastructure.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class SqlLoader {

    private SqlLoader() {}

    /**
     * Loads a SQL file from the classpath under /sql/<path>.
     *
     * @param path relative path under the /sql/ directory, e.g. "statistic/fanpage_overview.sql"
     * @return the full SQL string
     * @throws IllegalStateException if the file is not found or cannot be read
     */
    public static String load(String path) {
        String fullPath = "/sql/" + path;
        try (InputStream is = SqlLoader.class.getResourceAsStream(fullPath)) {
            Objects.requireNonNull(is, "SQL file not found on classpath: " + fullPath);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read SQL file: " + fullPath, e);
        }
    }
}
