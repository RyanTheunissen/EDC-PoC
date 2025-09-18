package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
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
    private static final int PIPE_SIZE = 1 << 20; // 1 MiB buffer

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

        Part part = new Part() {
            @Override
            public String name() {
                return "postgres-part";
            }

            @Override
            public InputStream openStream() {
                return createPipeAndStartCopy();
            }
        };

        return StreamResult.success(Stream.of(part));
    }

    @Override
    public void close() {
    }

    private InputStream createPipeAndStartCopy() {
        try {
            final PipedInputStream in = new PipedInputStream(PIPE_SIZE);
            final PipedOutputStream out = new PipedOutputStream(in);
            final AtomicBoolean closed = new AtomicBoolean(false);

            startCopyOutThread(out, closed);

            // Return a thin wrapper that properly propagates close()
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    return in.read();
                }
                @Override
                public int read(byte @NotNull [] b, int off, int len) throws IOException {
                    return in.read(b, off, len);
                }
                @Override
                public void close() throws IOException {
                    if (closed.compareAndSet(false, true)) {
                        in.close();
                    }
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize COPY pipe: " + e.getMessage(), e);
        }
    }

    private void startCopyOutThread(PipedOutputStream out, AtomicBoolean closed) {
        Thread t = new Thread(() -> runCopyOut(out, closed), "pg-copyout-" + requestId);
        t.setDaemon(true);
        t.start();
    }

    private void runCopyOut(PipedOutputStream out, AtomicBoolean closed) {
        try (Connection conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.user(), cfg.password())) {
            monitor.info("[PG SRC  " + requestId + "] jdbcUrl=" + cfg.jdbcUrl() + " sql=" + cfg.sql());
            conn.setAutoCommit(false);

            var baseConn = conn.unwrap(BaseConnection.class);
            var cm = new CopyManager(baseConn);

            var copySql = "COPY (" + cfg.sql() + ") TO STDOUT WITH (FORMAT CSV, HEADER true)";
            cm.copyOut(copySql, out);
        } catch (Exception e) {
            if (!closed.get()) {
                monitor.severe("[PG SRC  " + requestId + "] COPY OUT error: " + e.getMessage(), e);
            }
        } finally {
            try (out) {
                out.flush();
            } catch (IOException ignore) {
            }
        }
    }

}
