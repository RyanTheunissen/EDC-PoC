package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;

import java.sql.DriverManager;
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
                    + " truncate=" + cfg.truncateBeforeLoad());
            try (var conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.user(), cfg.password())) {
                monitor.info("[PG SINK " + requestId + "] connected");
                conn.setAutoCommit(false);

                if (cfg.truncateBeforeLoad()) {
                    monitor.info("[PG SINK " + requestId + "] TRUNCATE " + cfg.table());
                    try (var st = conn.createStatement()) {
                        st.executeUpdate("TRUNCATE TABLE " + cfg.table() + " RESTART IDENTITY");
                    }
                }

                var baseConn = conn.unwrap(org.postgresql.core.BaseConnection.class);
                var cm = new org.postgresql.copy.CopyManager(baseConn);

                var streamResult = source.openPartStream();
                if (streamResult.failed()) {
                    monitor.severe("[PG SINK " + requestId + "] openPartStream FAILED: " + streamResult.getFailureMessages());
                    return StreamResult.error("openPartStream failed: " + streamResult.getFailureMessages());
                }

                try (var parts = streamResult.getContent()) {
                    var part = parts.findFirst().orElse(null);
                    if (part == null) {
                        return StreamResult.error("no part");
                    }

                    monitor.info("[PG SINK " + requestId + "] COPY -> " + cfg.table());
                    try (part; var in = part.openStream()) {
                        long rows = cm.copyIn(
                                "COPY " + cfg.table() + " FROM STDIN WITH (FORMAT CSV, HEADER true)",
                                in
                        );
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
}
