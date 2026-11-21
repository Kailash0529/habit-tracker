package com.habittracker.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@Profile("prod")
public class DatabaseConfig {

    @Value("${SPRING_DATASOURCE_URL:${DATABASE_URL:}}")
    private String rawUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:${DB_USER:habituser}}")
    private String defaultUsername;

    @Value("${SPRING_DATASOURCE_PASSWORD:${DB_PASSWORD:habitpassword}}")
    private String defaultPassword;

    @Value("${DB_HOST:localhost}")
    private String defaultHost;

    @Value("${DB_PORT:5432}")
    private int defaultPort;

    @Value("${DB_NAME:habittracker}")
    private String defaultDatabase;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");

        String jdbcUrl;
        String username = defaultUsername;
        String password = defaultPassword;

        if (rawUrl != null && (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
            try {
                // Convert postgres:// or postgresql:// URI format into valid JDBC format
                String httpEquivalent = rawUrl.replaceFirst("^postgres(ql)?://", "http://");
                URI uri = new URI(httpEquivalent);

                String host = uri.getHost();
                int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                String path = uri.getPath(); // e.g. /habittracker
                String query = uri.getQuery();

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (query != null && !query.isEmpty()) {
                    jdbcUrl += "?" + query;
                }

                if (uri.getUserInfo() != null) {
                    String[] parts = uri.getUserInfo().split(":", 2);
                    if (parts.length > 0 && !parts[0].isEmpty()) {
                        username = parts[0];
                    }
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        password = parts[1];
                    }
                }
            } catch (Exception e) {
                jdbcUrl = rawUrl;
            }
        } else if (rawUrl != null && rawUrl.startsWith("jdbc:")) {
            jdbcUrl = rawUrl;
        } else {
            jdbcUrl = "jdbc:postgresql://" + defaultHost + ":" + defaultPort + "/" + defaultDatabase;
        }

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);

        return new HikariDataSource(config);
    }
}
