package org.example.edc.dataplane.postgres.pipeline;


import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.monitor.Monitor;


import java.util.Map;
import java.util.concurrent.ExecutorService;


import static org.example.edc.dataplane.postgres.pipeline.PostgresDataSchema.TYPE;


public class PostgresDataSinkFactory implements DataSinkFactory {
    private final PgClient client;
    private final Monitor monitor;
    private final ExecutorService executor;


    public PostgresDataSinkFactory(PgClient client, Monitor monitor, ExecutorService executor) {
        this.client = client;
        this.monitor = monitor;
        this.executor = executor;
    }


    @Override
    public boolean canHandle(Map<String, Object> destination) {
        return TYPE.equals(destination.get("type"));
    }


    @Override
    public DataSink createSink(Map<String, Object> destination) {
        return new PostgresDataSink(client, monitor, executor, destination);
    }
}
