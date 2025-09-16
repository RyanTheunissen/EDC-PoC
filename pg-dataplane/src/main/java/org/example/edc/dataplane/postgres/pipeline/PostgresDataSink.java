package org.example.edc.dataplane.postgres.pipeline;


import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.InputStreamDataChunk;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;


import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ExecutorService;


import static org.example.edc.dataplane.postgres.pipeline.PostgresDataSchema.*;
import static org.example.edc.dataplane.postgres.util.Props.first;


/** Writes CSV into Postgres using COPY target FROM STDIN WITH CSV HEADER. */
public class PostgresDataSink implements DataSink {
    private final PgClient client;
    private final Monitor monitor;
    private final Map<String, Object> destProps;


    public PostgresDataSink(PgClient client, Monitor monitor, Map<String, Object> destinationProperties) {
        this.client = client;
        this.monitor = monitor;
        this.destProps = destinationProperties;
    }


    @Override
    public StreamResult transfer(InputStreamDataChunk chunk) {
        try {
            String jdbcUrl = first(destProps, null, JDBC_URL_KEYS);
            String user = first(destProps, null, USER_KEYS);
            String pass = first(destProps, null, PASSWORD_KEYS);
            String passKey = first(destProps, null, PASSWORD_KEY_KEYS);
            String target = first(destProps, null, TARGET_TABLE_KEYS);
            Boolean truncate = first(destProps, Boolean.FALSE, TRUNCATE_KEYS);
            String ddl = first(destProps, null, CREATE_DDL_KEYS);


            if (target == null || target.isBlank()) {
                return StreamResult.error("Missing 'targetTable'");
            }


            try (Connection cn = client.open(jdbcUrl, user, pass, passKey)) {
                ensureTable(cn, target, ddl, truncate);
                try (InputStream in = chunk.getStream()) {
                    long rows = client.copyInCsv(cn, "COPY " + target + " FROM STDIN WITH CSV HEADER", in);
                    monitor.info("[pg-dataplane] Loaded rows=" + rows + " into " + target);
                    return StreamResult.success();
                }
            }
        } catch (Exception e) {
            monitor.severe("PostgresDataSink error: " + e.getMessage());
            return StreamResult.error(e.getMessage());
        }
    }