package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

class PostgresSourceFactory implements DataSourceFactory {
    private final Monitor monitor;

    PostgresSourceFactory(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public String supportedType() {
        return "postgres";
    }

    @Override
    public DataSource createSource(DataFlowStartMessage message) {
        DataAddress src = message.getSourceDataAddress();
        var cfg = PgCfg.fromAddress(src);
        return new PostgresSource(cfg, monitor, message.getProcessId());
    }

    @Override
    public Result<Void> validateRequest(DataFlowStartMessage message) {
        var src = message.getSourceDataAddress();
        if (src == null) {
            return Result.failure("Missing source DataAddress");
        }
        if (!"postgres".equalsIgnoreCase(src.getType())) {
            return Result.failure("Unsupported source type: " + src.getType());
        }
        return Result.success();
    }
}
