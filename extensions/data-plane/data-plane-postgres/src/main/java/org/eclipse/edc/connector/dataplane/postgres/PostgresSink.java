package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.InputStream;
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
            if (cfg.table() == null || cfg.table().isEmpty()) {
                return StreamResult.error("PostgresSink requires 'table' in destination DataAddress");
            }

            try (var conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.user(), cfg.password())) {
                conn.setAutoCommit(false);

                if (cfg.truncateBeforeLoad()) {
                    try (var st = conn.createStatement()) {
                        st.executeUpdate("TRUNCATE TABLE " + cfg.table() + " RESTART IDENTITY");
                    }
                }

                var baseConn = conn.unwrap(BaseConnection.class);
                var cm = new CopyManager(baseConn);

                var streamResult = source.openPartStream();
                if (streamResult.failed()) {
                    return StreamResult.error("Failed to open source stream: " + streamResult.getFailureMessages());
                }

                try (var parts = streamResult.getContent()) {
                    var part = parts.findFirst()
                            .orElseThrow(() -> new IllegalStateException("No part received from source"));

                    try (InputStream in = part.openStream()) {
                        var copySql = "COPY " + cfg.table() + " FROM STDIN WITH (FORMAT CSV, HEADER true)";
                        long rows = cm.copyIn(copySql, in);
                        conn.commit();
                        monitor.info("PostgresSink[" + requestId + "] copied " + rows + " rows into " + cfg.table());
                    }
                }

                return StreamResult.success(null);
            } catch (Exception e) {
                monitor.severe("PostgresSink[" + requestId + "] failed: " + e.getMessage(), e);
                return StreamResult.error("PostgresSink error: " + e.getMessage());
            }
        });
    }
}
