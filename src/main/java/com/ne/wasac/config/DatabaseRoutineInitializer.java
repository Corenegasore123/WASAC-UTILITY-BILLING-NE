package com.ne.wasac.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Installs PostgreSQL triggers and stored procedures after Hibernate DDL.
 * Requires preferQueryMode=simple on the JDBC URL so dollar-quoted bodies work.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DatabaseRoutineInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            int index = 0;
            for (String sql : DatabaseRoutines.STATEMENTS) {
                index++;
                try {
                    statement.execute(sql);
                    log.debug("Installed DB routine statement {}", index);
                } catch (Exception ex) {
                    log.error("Failed on routine statement {}: {}", index, ex.getMessage());
                    throw ex;
                }
            }
        }
        log.info("PostgreSQL billing routines installed successfully");
    }
}
