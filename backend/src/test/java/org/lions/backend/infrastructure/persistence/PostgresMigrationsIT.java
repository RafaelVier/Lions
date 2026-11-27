package org.lions.backend.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs only against the disposable PostgreSQL created by Database CI/CD. */
@EnabledIfEnvironmentVariable(named = "POSTGRES_MIGRATIONS_TEST", matches = "true")
class PostgresMigrationsIT {

    @Test
    void upgradesAnExistingDatabaseWithoutLosingDataAndCanRunAgain() throws Exception {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        String user = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");
        String schema = "migration_upgrade_ci";

        Flyway.configure().dataSource(url, user, password)
                .schemas(schema).defaultSchema(schema).target("1").load().migrate();

        try (Connection connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            try (var role = statement.executeQuery(
                    "SELECT rolsuper, rolcreatedb, rolcreaterole FROM pg_roles WHERE rolname = current_user")) {
                assertThat(role.next()).isTrue();
                assertThat(role.getBoolean("rolsuper")).isFalse();
                assertThat(role.getBoolean("rolcreatedb")).isFalse();
                assertThat(role.getBoolean("rolcreaterole")).isFalse();
            }
            statement.executeUpdate("""
                    INSERT INTO migration_upgrade_ci.tb_users
                        (username, email, password, role, created_at)
                    VALUES ('ci_existing_user', 'ci@example.test', 'ci-test-hash', 'ROLE_VIEWER', CURRENT_TIMESTAMP)
                    """);
        }

        Flyway latest = Flyway.configure().dataSource(url, user, password)
                .schemas(schema).defaultSchema(schema).load();
        latest.migrate();
        latest.validate();
        assertThat(latest.info().pending()).isEmpty();
        assertThat(latest.migrate().migrationsExecuted).isZero();

        try (Connection connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT username FROM migration_upgrade_ci.tb_users WHERE username = 'ci_existing_user'
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("username")).isEqualTo("ci_existing_user");
        }
    }
}
