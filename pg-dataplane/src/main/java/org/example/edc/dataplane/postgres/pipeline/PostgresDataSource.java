package org.example.edc.dataplane.postgres.pipeline;
import org.eclipse.edc.spi.monitor.Monitor;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import static org.example.edc.dataplane.postgres.pipeline.PostgresDataSchema.*;
import static org.example.edc.dataplane.postgres.util.Props.first;


/** Streams CSV from Postgres using COPY (SELECT …) TO STDOUT WITH CSV [HEADER]. */
public class PostgresDataSource implements DataSource {
    private final PgClient client;
    private final Monitor monitor;
    private final Map<String, Object> props;
    private final ExecutorService pool = Executors.newCachedThreadPool();


    public PostgresDataSource(PgClient client, Monitor monitor, Map<String, Object> properties) {
        this.client = client;
        this.monitor = monitor;
        this.props = properties;
    }


    @Override
    public StreamResult openPartStream() {
        try {
            String jdbcUrl = first(props, null, JDBC_URL_KEYS);
            String user = first(props, null, USER_KEYS);
            String pass = first(props, null, PASSWORD_KEYS);
            String passKey = first(props, null, PASSWORD_KEY_KEYS);
            String table = first(props, null, TABLE_KEYS);
            String query = first(props, null, QUERY_KEYS);
            Boolean header = first(props, Boolean.TRUE, CSV_HEADER_KEYS);


            if ((query == null || query.isBlank()) && (table == null || table.isBlank())) {
                return StreamResult.error("Either 'query' or 'table' must be provided");
            }


            final Connection cn = client.open(jdbcUrl, user, pass, passKey);
            String copySql = (query != null && !query.isBlank())
                    ? ("COPY (" + query + ") TO STDOUT WITH CSV" + (header ? " HEADER" : ""))
                    : ("COPY " + table + " TO STDOUT WITH CSV" + (header ? " HEADER" : ""));


// Pump COPY OUT on a background thread into a pipe InputStream
            var pipe = new java.io.PipedInputStream(64 * 1024);
            var out = new java.io.PipedOutputStream(pipe);
            pool.submit(() -> {
                try (out; cn) { client.copyOutCsv(cn, copySql, out); }
                catch (Exception e) { monitor.severe("COPY OUT failed: " + e.getMessage()); }
            });


            var chunk = new InputStreamDataChunk(pipe, 32 * 1024, StandardCharsets.UTF_8);
            return StreamResult.success(chunk);
        } catch (Exception e) {
            monitor.severe("PostgresDataSource error: " + e.getMessage());
            return StreamResult.error(e.getMessage());
        }
    }
}