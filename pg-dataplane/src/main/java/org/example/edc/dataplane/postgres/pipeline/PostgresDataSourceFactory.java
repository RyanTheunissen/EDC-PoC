package org.example.edc.dataplane.postgres.pipeline;


import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.monitor.Monitor;


import java.util.Map;


import static org.example.edc.dataplane.postgres.pipeline.PostgresDataSchema.TYPE;


public class PostgresDataSourceFactory implements DataSourceFactory {
    private final PgClient client;
    private final Monitor monitor;
    private final java.util.concurrent.ExecutorService exec;


    public PostgresDataSourceFactory(PgClient client, Monitor monitor, java.util.concurrent.ExecutorService exec) {
        this.client = client;
        this.monitor = monitor;
        this.exec = exec;
    }


    @Override
    public boolean canHandle(Map<String, Object> sourceDataAddress) {
        return TYPE.equals(sourceDataAddress.get("type"));
    }


    @Override
    public DataSource createSource(Map<String, Object> sourceDataAddress) {
        return new PostgresDataSource(client, monitor, sourceDataAddress);
    }
}