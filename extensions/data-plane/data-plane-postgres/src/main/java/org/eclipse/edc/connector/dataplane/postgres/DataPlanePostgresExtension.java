package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension(value = "Data Plane Postgres Extension")
public class DataPlanePostgresExtension implements ServiceExtension {

    @Inject private PipelineService pipelineService;
    @Inject private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor.info("Initializing Postgres data-plane extension");
        pipelineService.registerFactory(new PostgresSourceFactory(monitor));
        pipelineService.registerFactory(new PostgresSinkFactory(monitor));
        monitor.info("Postgres factories registered");
    }
}
