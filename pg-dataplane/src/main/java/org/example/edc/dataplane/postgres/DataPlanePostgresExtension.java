package org.example.edc.dataplane.postgres;


import org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.example.edc.dataplane.postgres.pipeline.PgClient;
import org.example.edc.dataplane.postgres.pipeline.PostgresDataSinkFactory;
import org.example.edc.dataplane.postgres.pipeline.PostgresDataSourceFactory;


@Extension(value = DataPlanePostgresExtension.NAME)
public class DataPlanePostgresExtension implements org.eclipse.edc.spi.system.ServiceExtension {
    public static final String NAME = "Data Plane — Postgres JDBC";


    @Inject private PipelineService pipelineService;
    @Inject private DataTransferExecutorServiceContainer executors;
    @Inject private Vault vault;


    @Override
    public String name() { return NAME; }


    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var client = new PgClient(monitor, vault);


        pipelineService.registerFactory(new PostgresDataSourceFactory(client, monitor, executors.getExecutorService()));
        pipelineService.registerFactory(new PostgresDataSinkFactory(client, monitor, executors.getExecutorService()));


        monitor.info("[pg-dataplane] Registered JDBC Postgres source+sink (type=JdbcData)");
    }
}