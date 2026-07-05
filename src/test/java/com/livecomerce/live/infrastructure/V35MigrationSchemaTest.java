package com.livecomerce.live.infrastructure;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the V35 migration adds the enrichment columns to
 * {@code live_summaries} as NULLABLE (historical rows keep null, no backfill).
 *
 * Uses an in-memory H2 database, same approach as {@link V34MigrationSchemaTest}.
 */
class V35MigrationSchemaTest {

    private static final String JDBC_URL = "jdbc:h2:mem:v35test;DB_CLOSE_DELAY=-1";

    @Test
    void v35Migration_addsNullableEnrichmentColumns() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            setupPrerequisiteTables(conn);
            runV35Migration(conn);

            var columns = nullableColumns(conn, "LIVE_SUMMARIES");
            assertThat(columns).containsEntry("CURRENCY", true);
            assertThat(columns).containsEntry("UNITS_SOLD", true);
            assertThat(columns).containsEntry("TOTAL_ALLOCATED", true);

            insertHistoricalRowWithoutNewColumns(conn);
        }
    }

    private void setupPrerequisiteTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE lives (
                        id UUID PRIMARY KEY
                    )
                    """);
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
        }
    }

    private void runV35Migration(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE live_summaries ADD COLUMN currency        VARCHAR(3)");
            st.execute("ALTER TABLE live_summaries ADD COLUMN units_sold      INTEGER");
            st.execute("ALTER TABLE live_summaries ADD COLUMN total_allocated INTEGER");
        }
    }

    private Map<String, Boolean> nullableColumns(Connection conn, String tableName) throws SQLException {
        Map<String, Boolean> result = new HashMap<>();
        var meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                result.put(rs.getString("COLUMN_NAME"), rs.getInt("NULLABLE") == java.sql.DatabaseMetaData.columnNullable);
            }
        }
        return result;
    }

    private void insertHistoricalRowWithoutNewColumns(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO lives (id) VALUES (RANDOM_UUID())");
            st.execute("""
                    INSERT INTO live_summaries (live_id, seller_id, duration_seconds, peak_viewers)
                    SELECT id, RANDOM_UUID(), 3600, 42 FROM lives
                    """);
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT currency, units_sold, total_allocated FROM live_summaries")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("currency")).isNull();
            assertThat(rs.getObject("units_sold")).isNull();
            assertThat(rs.getObject("total_allocated")).isNull();
        }
    }
}
