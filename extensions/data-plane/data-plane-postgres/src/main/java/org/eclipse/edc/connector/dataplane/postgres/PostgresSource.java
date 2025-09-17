package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.atomic.AtomicBoolean;
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
        if (cfg.sql() == null || cfg.sql().isEmpty()) {
            return StreamResult.error("PostgresSource requires 'sql' in source DataAddress");
        }

        // We will start COPY OUT only when the stream is opened (lazy), so we return a Part that does that.
        Part part = new Part() {
            @Override
            public String name() {
                return "postgres-part";
            }

            @Override
            public InputStream openStream() {
                final int PIPE_SIZE = 1 << 20; // 1 MiB buffer to avoid thread stalls
                try {
                    final PipedInputStream in = new PipedInputStream(PIPE_SIZE);
                    final PipedOutputStream out = new PipedOutputStream(in);
                    final AtomicBoolean closed = new AtomicBoolean(false);

                    Thread t = new Thread(() -> {
                        try (Connection conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.user(), cfg.password())) {
                            monitor.info("[PG SRC  " + requestId + "] jdbcUrl=" + cfg.jdbcUrl() + " sql=" + cfg.sql());
                            conn.setAutoCommit(false);

                            var baseConn = conn.unwrap(BaseConnection.class);
                            var cm = new CopyManager(baseConn);

                            var copySql = "COPY (" + cfg.sql() + ") TO STDOUT WITH (FORMAT CSV, HEADER true)";
                            // Use the CopyManager variant that writes directly to an OutputStream
                            cm.copyOut(copySql, out);
                        } catch (Exception e) {
                            if (!closed.get()) {
                                monitor.severe("[PG SRC  " + requestId + "] COPY OUT error: " + e.getMessage(), e);
                            }
                        } finally {
                            try { out.close(); } catch (IOException ignore) {}
                        }
                    }, "pg-copyout-" + requestId);

                    t.setDaemon(true);
                    t.start();

                    // Return the read end to the sink (copyIn will consume it)
                    return new InputStream() {
                        @Override
                        public int read() throws IOException {
                            return in.read();
                        }
                        @Override
                        public int read(byte[] b, int off, int len) throws IOException {
                            return in.read(b, off, len);
                        }
                        @Override
                        public void close() throws IOException {
                            if (closed.compareAndSet(false, true)) {
                                try { in.close(); } catch (IOException ignore) {}
                                // 'out' is closed by the COPY thread in finally{}
                            }
                        }
                    };

                } catch (IOException e) {
                    throw new RuntimeException("Failed to initialize COPY pipe: " + e.getMessage(), e);
                }
            }
        };

        return StreamResult.success(Stream.of(part));
    }

    @Override
    public void close() {
        // NO-OP: the COPY thread owns its JDBC connection and closes it when done.
    }
}
