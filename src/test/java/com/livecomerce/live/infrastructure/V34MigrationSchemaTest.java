package com.livecomerce.live.infrastructure;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the V34 migration creates {@code live_summaries} and
 * {@code live_summary_orders} with the expected primary keys, foreign keys,
 * and unique constraint.
 *
 * Uses an in-memory H2 database to avoid needing a running PostgreSQL instance.
 * The migration DDL is semantically identical for both databases (types
 * adapted where H2 syntax differs from Postgres, e.g. TIMESTAMPTZ ->
 * TIMESTAMP WITH TIME ZONE).
 */
class V34MigrationSchemaTest {

    private static final String JDBC_URL = "jdbc:h2:mem:v34test;DB_CLOSE_DELAY=-1";

    @Test
    void v34Migration_createsLiveSummaryTables_withExpectedConstraints() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            setupPrerequisiteLivesTable(conn);
            runV34Migration(conn);

            assertThat(primaryKeyColumns(conn, "LIVE_SUMMARIES"))
                    .as("live_summaries PK must be live_id")
                    .containsExactly("LIVE_ID");

            assertThat(primaryKeyColumns(conn, "LIVE_SUMMARY_ORDERS"))
                    .as("live_summary_orders PK must be id")
                    .containsExactly("ID");

            assertThat(foreignKeyTargets(conn, "LIVE_SUMMARIES"))
                    .as("live_summaries.live_id must reference lives(id)")
                    .contains("LIVES");

            assertThat(foreignKeyTargets(conn, "LIVE_SUMMARY_ORDERS"))
                    .as("live_summary_orders.live_id must reference live_summaries(live_id)")
                    .contains("LIVE_SUMMARIES");

            assertThat(hasUniqueConstraint(conn, "LIVE_SUMMARY_ORDERS", Set.of("LIVE_ID", "ORDER_ID")))
                    .as("live_summary_orders must have UNIQUE(live_id, order_id)")
                    .isTrue();

            insertSampleRow(conn);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void setupPrerequisiteLivesTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS lives (
                        id UUID PRIMARY KEY
                    )
                    """);
        }
    }

    private void runV34Migration(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE live_summaries (
                        live_id          UUID                     PRIMARY KEY REFERENCES lives(id),
                        seller_id        UUID                     NOT NULL,
                        store_id         UUID,
                        started_at       TIMESTAMP WITH TIME ZONE,
                        ended_at         TIMESTAMP WITH TIME ZONE,
                        duration_seconds BIGINT                   NOT NULL,
                        peak_viewers     INTEGER                  NOT NULL,
                        total_sales      NUMERIC(10,2)            NOT NULL DEFAULT 0,
                        order_count      INTEGER                  NOT NULL DEFAULT 0,
                        created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
                    )
                    """);
            st.execute("""
                    CREATE TABLE live_summary_orders (
                        id          UUID                     PRIMARY KEY,
                        live_id     UUID                     NOT NULL REFERENCES live_summaries(live_id) ON DELETE CASCADE,
                        order_id    UUID                     NOT NULL,
                        buyer_id    UUID                     NOT NULL,
                        item_names  TEXT,
                        order_total NUMERIC(10,2)            NOT NULL,
                        created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                        UNIQUE (live_id, order_id)
                    )
                    """);
            st.execute("CREATE INDEX idx_live_summary_orders_live ON live_summary_orders(live_id)");
        }
    }

    private Set<String> primaryKeyColumns(Connection conn, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        var meta = conn.getMetaData();
        try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }

    private Set<String> foreignKeyTargets(Connection conn, String tableName) throws SQLException {
        Set<String> targets = new HashSet<>();
        var meta = conn.getMetaData();
        try (ResultSet rs = meta.getImportedKeys(null, null, tableName)) {
            while (rs.next()) {
                targets.add(rs.getString("PKTABLE_NAME"));
            }
        }
        return targets;
    }

    private boolean hasUniqueConstraint(Connection conn, String tableName, Set<String> expectedColumns)
            throws SQLException {
        var meta = conn.getMetaData();
        java.util.Map<String, Set<String>> columnsByIndex = new java.util.HashMap<>();
        try (ResultSet rs = meta.getIndexInfo(null, null, tableName, true, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                if (indexName == null || columnName == null) {
                    continue;
                }
                columnsByIndex.computeIfAbsent(indexName, k -> new HashSet<>()).add(columnName);
            }
        }
        return columnsByIndex.values().stream().anyMatch(cols -> cols.equals(expectedColumns));
    }

    private void insertSampleRow(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    INSERT INTO lives (id) VALUES (RANDOM_UUID())
                    """);
            st.execute("""
                    INSERT INTO live_summaries (live_id, seller_id, duration_seconds, peak_viewers)
                    SELECT id, RANDOM_UUID(), 3600, 42 FROM lives
                    """);
        }
    }
}
