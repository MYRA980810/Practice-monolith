package com.livecomerce.live.infrastructure;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that V31 migration backfill logic correctly converts
 * is_pinned = true rows to status = 'PINNED'.
 *
 * Uses an in-memory H2 database to avoid needing a running PostgreSQL instance.
 * The migration SQL is semantically identical for both databases.
 */
class V31MigrationBackfillTest {

    private static final String JDBC_URL = "jdbc:h2:mem:v31test;DB_CLOSE_DELAY=-1";

    @Test
    void v31Migration_backfillsPinnedRows_andDropsIsPinnedColumn() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            setupPreMigrationTable(conn);
            seedTestData(conn);

            int pinnedCountBefore = countPinnedRows(conn);

            runV31Migration(conn);

            int pinnedCountAfter = countStatusPinned(conn);

            assertThat(pinnedCountAfter)
                    .as("Every row with is_pinned=true before V31 must have status='PINNED' after V31")
                    .isEqualTo(pinnedCountBefore);

            assertThat(columnExists(conn, "is_pinned"))
                    .as("Column is_pinned must be gone after V31")
                    .isFalse();

            assertThat(columnExists(conn, "status"))
                    .as("Column status must exist after V31")
                    .isTrue();

            int availableCount = countStatusAvailable(conn);
            assertThat(availableCount)
                    .as("Non-pinned rows must have status='AVAILABLE'")
                    .isEqualTo(2);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void setupPreMigrationTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS live_products (
                        id         UUID         NOT NULL PRIMARY KEY,
                        is_pinned  BOOLEAN      NOT NULL DEFAULT FALSE,
                        stock_sold INTEGER      NOT NULL DEFAULT 0,
                        position   INTEGER      NOT NULL DEFAULT 0
                    )
                    """);
        }
    }

    private void seedTestData(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            // 1 pinned row
            st.execute("INSERT INTO live_products (id, is_pinned) VALUES (RANDOM_UUID(), TRUE)");
            // 2 available rows
            st.execute("INSERT INTO live_products (id, is_pinned) VALUES (RANDOM_UUID(), FALSE)");
            st.execute("INSERT INTO live_products (id, is_pinned) VALUES (RANDOM_UUID(), FALSE)");
        }
    }

    private int countPinnedRows(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM live_products WHERE is_pinned = TRUE")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void runV31Migration(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE live_products ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'");
            st.execute("UPDATE live_products SET status = 'PINNED' WHERE is_pinned = TRUE");
            st.execute("ALTER TABLE live_products DROP COLUMN is_pinned");
        }
    }

    private int countStatusPinned(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM live_products WHERE status = 'PINNED'")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countStatusAvailable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM live_products WHERE status = 'AVAILABLE'")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private boolean columnExists(Connection conn, String columnName) throws SQLException {
        var meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, "LIVE_PRODUCTS", columnName.toUpperCase())) {
            return rs.next();
        }
    }
}
