package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.sql.DriverManager;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

class PostgresSink implements DataSink {
    private final PgCfg cfg;
    private final Monitor monitor;
    private final String requestId;

    PostgresSink(PgCfg cfg, Monitor monitor, String requestId) {
        this.cfg = cfg;
        this.monitor = monitor;
        this.requestId = requestId;
    }

    @Override
    public CompletableFuture<StreamResult<Object>> transfer(DataSource source) {
        return CompletableFuture.supplyAsync(() -> {
            monitor.info("[PG SINK " + requestId + "] jdbcUrl=" + cfg.jdbcUrl()
                    + " table=" + cfg.table()
                    + " truncate=" + cfg.truncateBeforeLoad()
                    + " createIfMissing=" + cfg.createIfMissing());
            try (var conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.user(), cfg.password())) {
                monitor.info("[PG SINK " + requestId + "] connected");
                conn.setAutoCommit(false);

                ensureTableExists(conn);

                if (cfg.truncateBeforeLoad()) {
                    monitor.info("[PG SINK " + requestId + "] TRUNCATE " + cfg.table());
                    try (var st = conn.createStatement()) {
                        st.executeUpdate("TRUNCATE TABLE " + cfg.table() + " RESTART IDENTITY");
                    }
                }

                var baseConn = conn.unwrap(BaseConnection.class);
                var cm = new CopyManager(baseConn);

                var streamResult = source.openPartStream();
                if (streamResult.failed()) {
                    var msg = "openPartStream failed: " + streamResult.getFailureMessages();
                    monitor.severe("[PG SINK " + requestId + "] " + msg);
                    conn.rollback();
                    return StreamResult.error(msg);
                }

                try (var parts = streamResult.getContent()) {
                    var part = parts.findFirst().orElse(null);
                    if (part == null) {
                        conn.rollback();
                        return StreamResult.error("no part");
                    }

                    var copySql = buildCopySql();
                    monitor.info("[PG SINK " + requestId + "] COPY -> " + copySql);
                    try (part; var in = part.openStream()) {
                        long rows = cm.copyIn(copySql, in);
                        conn.commit();
                        monitor.info("[PG SINK " + requestId + "] COPY done. rows=" + rows);
                    }
                }

                return StreamResult.success(null);
            } catch (Exception e) {
                monitor.severe("[PG SINK " + requestId + "] ERROR: " + e.getMessage(), e);
                return StreamResult.error(e.getMessage());
            }
        });
    }

    private void ensureTableExists(java.sql.Connection conn) throws Exception {
        String table = cfg.table();
        String schema = "public";
        String name = table;
        var parts = table.split("\\.", 2);
        if (parts.length == 2) { schema = parts[0]; name = parts[1]; }

        try (var rs = conn.getMetaData().getTables(null, schema, name, null)) {
            if (rs.next()) {
                return;
            }
        }

        if (!cfg.createIfMissing()) {
            throw new IllegalStateException("Destination table not found: " + table +
                    ". Set createIfMissing=true or pre-create the table.");
        }

        String ddlInner;
        if (cfg.columnsDdl() != null && !cfg.columnsDdl().isEmpty()) {
            ddlInner = cfg.columnsDdl();
        } else if (cfg.columns() != null && !cfg.columns().isEmpty()) {
            var cols = Arrays.stream(cfg.columns().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(c -> c + " TEXT")
                    .toList();
            if (cols.isEmpty()) {
                throw new IllegalStateException("createIfMissing=true but 'columns' is empty; provide columns or columnsDdl");
            }
            ddlInner = String.join(", ", cols);
        } else {
            throw new IllegalStateException("createIfMissing=true but neither 'columnsDdl' nor 'columns' provided");
        }

        var fq = schema + "." + name;
        var ddl = "CREATE TABLE " + fq + " (" + ddlInner + ")";
        monitor.info("[PG SINK " + requestId + "] CREATE TABLE -> " + ddl);
        try (var st = conn.createStatement()) {
            st.executeUpdate(ddl);
        }
    }

    private String buildCopySql() {
        var cols = cfg.columns();
        if (cols != null && !cols.isBlank()) {
            return "COPY " + cfg.table() + " (" + cols + ") FROM STDIN WITH (FORMAT CSV, HEADER true)";
        }
        return "COPY " + cfg.table() + " FROM STDIN WITH (FORMAT CSV, HEADER true)";
    }
}
