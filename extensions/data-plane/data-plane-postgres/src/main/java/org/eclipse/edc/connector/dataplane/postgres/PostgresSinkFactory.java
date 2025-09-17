package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

class PostgresSinkFactory implements DataSinkFactory {
    private final Monitor monitor;

    PostgresSinkFactory(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public String supportedType() {
        return "postgres";
    }

    @Override
    public DataSink createSink(DataFlowStartMessage message) {
        DataAddress dest = message.getDestinationDataAddress();
        var cfg = PgCfg.fromAddress(dest);
        return new PostgresSink(cfg, monitor, message.getProcessId());
    }

    @Override
    public Result<Void> validateRequest(DataFlowStartMessage message) {
        var dest = message.getDestinationDataAddress();
        if (dest == null) {
            return Result.failure("Missing destination DataAddress");
        }
        if (!"postgres".equalsIgnoreCase(dest.getType())) {
            return Result.failure("Unsupported destination type: " + dest.getType());
        }
        return Result.success();
    }
}
