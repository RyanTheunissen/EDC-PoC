package org.example.edc.dataplane.postgres.pipeline;


import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.monitor.Monitor;


import java.util.Map;


import static org.example.edc.dataplane.postgres.pipeline.PostgresDataSchema.TYPE;


public class PostgresDataSinkFactory implements DataSinkFactory {
    private final PgClient client;
    private final Monitor monitor;
    private final java.util.concurrent.ExecutorService exec;


    public PostgresDataSinkFactory(PgClient client, Monitor monitor, java.util.concurrent.ExecutorService exec) {
        this.client = client;
        this.monitor = monitor;
        this.exec = exec;
    }


    @Override
    public boolean canHandle(Map<String, Object> destination) {
        return TYPE.equals(destination.get("type"));
    }


    @Override
    public DataSink createSink(Map<String, Object> destination) {
        return new PostgresDataSink(client, monitor, destination);
    }
}