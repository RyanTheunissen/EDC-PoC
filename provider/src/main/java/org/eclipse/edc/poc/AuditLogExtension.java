package org.eclipse.edc.poc;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;


@Extension(value = "Audit Logging Extension")
public class AuditLogExtension implements ServiceExtension {

    @Inject
    private Monitor monitor;

    @Inject
    private EventRouter eventRouter;

    @Override
    public String name() {
        return "Audit Logging Extension";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor.info("AUDIT: AuditLogExtension initialized");

        eventRouter.register(Event.class, new AuditEventSubscriber(monitor));
    }


    static class AuditEventSubscriber implements EventSubscriber {

        private final Monitor monitor;

        AuditEventSubscriber(Monitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public void on(EventEnvelope eventEnvelope) {
            var payload = eventEnvelope.getPayload();
            var type = payload != null ? payload.getClass().getSimpleName() : "null";
            var id = eventEnvelope.getId();
            var at = eventEnvelope.getAt();

            monitor.info(String.format(
                    "AUDIT: eventType=%s eventId=%s at=%d",
                    type, id, at
            ));
        }
    }
}
