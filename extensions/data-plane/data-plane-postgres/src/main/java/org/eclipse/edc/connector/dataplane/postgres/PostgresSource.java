package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.postgresql.copy.CopyManager;
import org.postgresql.copy.CopyOut;
import org.postgresql.core.BaseConnection;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.stream.Stream;

class PostgresSource implements DataSource {
    private final PgCfg cfg;
    private final Monitor monitor;
    private final String requestId;

    PostgresSource(PgCfg cfg, Monitor monitor, String requestId) {
        this.cfg = cfg;
        this.monitor = monitor;
        this.requestId = requestId;
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        monitor.info("[PG SRC  " + requestId + "] jdbcUrl=" + cfg.jdbcUrl() + " sql=" + cfg.sql());
        try {
            // Create a fresh connection per stream and keep it owned by the InputStream
            final Connection conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.user(), cfg.password());
            monitor.info("[PG SRC  " + requestId + "] connected");
            conn.setAutoCommit(false);

            var baseConn = conn.unwrap(BaseConnection.class);
            var cm = new CopyManager(baseConn);

            if (cfg.sql() == null || cfg.sql().isEmpty()) {
                try { conn.close(); } catch (Exception ignore) {}
                return StreamResult.error("PostgresSource requires 'sql' in source DataAddress");
            }

            final String copySql = "COPY (" + cfg.sql() + ") TO STDOUT WITH (FORMAT CSV, HEADER true)";
            final CopyOut copyOut = cm.copyOut(copySql);

            InputStream inputStream = new InputStream() {
                private byte[] buffer = null;
                private int pos = 0;
                private boolean closed = false;

                @Override
                public int read() throws IOException {
                    if (buffer == null || pos >= buffer.length) {
                        try {
                            buffer = copyOut.readFromCopy(); // null => EOF
                            if (buffer == null) return -1;
                            pos = 0;
                        } catch (Exception e) {
                            throw new IOException(e);
                        }
                    }
                    return buffer[pos++] & 0xFF;
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    int first = read();
                    if (first == -1) return -1;
                    b[off] = (byte) first;
                    int count = 1;
                    while (count < len) {
                        int next = read();
                        if (next == -1) break;
                        b[off + count++] = (byte) next;
                    }
                    return count;
                }

                @Override
                public void close() throws IOException {
                    if (closed) return;
                    closed = true;
                    try { copyOut.cancelCopy(); } catch (Exception ignore) {}
                    try { conn.close(); } catch (Exception ignore) {}
                }
            };

            Part part = new Part() {
                @Override
                public String name() { return "postgres-part"; }
                @Override
                public InputStream openStream() { return inputStream; }
            };

            return StreamResult.success(Stream.of(part));
        } catch (Exception e) {
            monitor.severe("PostgresSource[" + requestId + "] failed: " + e.getMessage(), e);
            return StreamResult.error("PostgresSource error: " + e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        // NO-OP: the InputStream owns closing the COPY and the JDBC connection.
    }
}
