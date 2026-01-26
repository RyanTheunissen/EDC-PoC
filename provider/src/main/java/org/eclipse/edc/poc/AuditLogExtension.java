package org.eclipse.edc.poc;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationTerminated;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessDeprovisioned;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessInitiated;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessTerminated;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Extension(value = "Audit Logging Extension")
public class AuditLogExtension implements ServiceExtension {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    @Inject
    private Monitor monitor;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private TransferProcessStore transferProcessStore;

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Override
    public String name() {
        return "Audit Logging Extension";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var participantId = context.getSetting("edc.participant.id", "unknown");

        AUDIT.info("AUDIT: AuditLogExtension initialized for participantId={}", participantId);

        eventRouter.register(
                Event.class,
                new AuditEventSubscriber(participantId, transferProcessStore, contractNegotiationStore)
        );
    }

    static class ContractInfo {
        final String consumerId;
        final String providerId;
        final String assetId;

        ContractInfo(String consumerId, String providerId, String assetId) {
            this.consumerId = consumerId;
            this.providerId = providerId;
            this.assetId = assetId;
        }
    }

    static class AuditEventSubscriber implements EventSubscriber {

        private final String participantId;
        private final TransferProcessStore transferProcessStore;
        private final ContractNegotiationStore contractNegotiationStore;

        private final Map<String, ContractInfo> contractsById = new ConcurrentHashMap<>();

        AuditEventSubscriber(String participantId,
                             TransferProcessStore transferProcessStore,
                             ContractNegotiationStore contractNegotiationStore) {
            this.participantId = participantId;
            this.transferProcessStore = transferProcessStore;
            this.contractNegotiationStore = contractNegotiationStore;
        }

        private String mapNegotiationState(int code) {
            return switch (code) {
                case 50 -> "INITIAL";
                case 100 -> "REQUESTING";
                case 200 -> "REQUESTED";
                case 300 -> "OFFERING";
                case 400 -> "OFFERED";
                case 700 -> "ACCEPTING";
                case 800 -> "ACCEPTED";
                case 825 -> "AGREEING";
                case 850 -> "AGREED";
                case 1050 -> "VERIFYING";
                case 1100 -> "VERIFIED";
                case 1150 -> "FINALIZING";
                case 1200 -> "FINALIZED";
                case 1300 -> "TERMINATING";
                case 1400 -> "TERMINATED";
                default -> "UNKNOWN(" + code + ")";
            };
        }

        private String mapTransferState(int code) {
            return switch (code) {
                case 100 -> "INITIAL";
                case 200 -> "PROVISIONING";
                case 300 -> "PROVISIONED";
                case 400 -> "REQUESTING";
                case 500 -> "STARTED";
                case 600 -> "COMPLETING";
                case 700 -> "COMPLETED";
                case 800 -> "TERMINATING";
                case 900 -> "TERMINATED";
                case 1000 -> "ERROR";
                default -> "UNKNOWN(" + code + ")";
            };
        }

        @Override
        public void on(EventEnvelope eventEnvelope) {
            var payload = eventEnvelope.getPayload();

            if (payload instanceof ContractNegotiationFinalized finalized) {
                handleContractFinalized(finalized, eventEnvelope);
                return;
            }

            if (payload instanceof ContractNegotiationTerminated terminated) {
                handleContractTerminated(terminated, eventEnvelope);
                return;
            }

            if (payload instanceof TransferProcessInitiated initiated) {
                logTransfer("INITIATED", initiated.getTransferProcessId(), eventEnvelope);
                return;
            }

            if (payload instanceof TransferProcessStarted started) {
                logTransfer("STARTED", started.getTransferProcessId(), eventEnvelope);
                return;
            }

            if (payload instanceof TransferProcessCompleted completed) {
                logTransfer("COMPLETED", completed.getTransferProcessId(), eventEnvelope);
                return;
            }

            if (payload instanceof TransferProcessTerminated terminatedTransfer) {
                logTransfer("TERMINATED", terminatedTransfer.getTransferProcessId(), eventEnvelope);
                return;
            }

            if (payload instanceof TransferProcessDeprovisioned deprovisioned) {
                logTransfer("DEPROVISIONED", deprovisioned.getTransferProcessId(), eventEnvelope);
                return;
            }

            var type = (payload != null) ? payload.getClass().getSimpleName() : "null";
            AUDIT.info("AUDIT: participant={} eventType={} eventId={} at={}",
                    participantId, type, eventEnvelope.getId(), eventEnvelope.getAt());
        }

        private void handleContractFinalized(ContractNegotiationFinalized finalized, EventEnvelope envelope) {
            ContractAgreement agreement = finalized.getContractAgreement();

            var contractId = agreement.getId();
            var consumerId = agreement.getConsumerId();
            var providerId = agreement.getProviderId();
            var assetId = agreement.getAssetId();

            contractsById.put(contractId, new ContractInfo(consumerId, providerId, assetId));

            AUDIT.info("AUDIT-CONTRACT: participant={} eventType=ContractNegotiationFinalized eventId={} at={} contractId={} consumerId={} providerId={} assetId={}",
                    participantId, envelope.getId(), envelope.getAt(), contractId, consumerId, providerId, assetId);
        }

        private void handleContractTerminated(ContractNegotiationTerminated terminated, EventEnvelope envelope) {
            var negotiationId = terminated.getContractNegotiationId();

            var negotiation = contractNegotiationStore.findById(negotiationId);

            String counterPartyId = "unknown";
            String counterPartyAddress = "unknown";
            String negotiationSide = "unknown";
            String state = "unknown";

            if (negotiation != null) {
                counterPartyId = negotiation.getCounterPartyId();
                counterPartyAddress = negotiation.getCounterPartyAddress();
                negotiationSide = negotiation.getType().name();
                state = mapNegotiationState(negotiation.getState());
            }

            String reason = null;
            try {
                var method = terminated.getClass().getMethod("getReason");
                Object value = method.invoke(terminated);
                if (value != null) {
                    reason = value.toString();
                }
            } catch (Exception ignored) {
            }

            if (reason != null) {
                AUDIT.info("AUDIT-CONTRACT-FAILED: participant={} eventType=ContractNegotiationTerminated eventId={} at={} negotiationId={} side={} state={} counterPartyId={} counterPartyAddress={} reason=\"{}\"",
                        participantId, envelope.getId(), envelope.getAt(), negotiationId, negotiationSide, state, counterPartyId, counterPartyAddress, reason);
            } else {
                AUDIT.info("AUDIT-CONTRACT-FAILED: participant={} eventType=ContractNegotiationTerminated eventId={} at={} negotiationId={} side={} state={} counterPartyId={} counterPartyAddress={}",
                        participantId, envelope.getId(), envelope.getAt(), negotiationId, negotiationSide, state, counterPartyId, counterPartyAddress);
            }
        }

        private void logTransfer(String phase, String transferProcessId, EventEnvelope<?> envelope) {
            TransferProcess tp = transferProcessStore.findById(transferProcessId);
            var eventId = envelope.getId();
            var at = envelope.getAt();

            if (tp == null) {
                AUDIT.info("AUDIT-TRANSFER: participant={} phase={} eventId={} at={} transferId={} (transfer process not found in store)",
                        participantId, phase, eventId, at, transferProcessId);
                return;
            }

            String contractId = tp.getContractId();
            ContractInfo ci = contractsById.get(contractId);

            String consumerId = ci != null ? ci.consumerId : null;
            String providerId = ci != null ? ci.providerId : null;
            String assetId = ci != null ? ci.assetId : null;

            DataAddress dest = tp.getDataDestination();

            String destType = dest != null ? dest.getType() : null;
            String bucket = dest != null ? dest.getStringProperty("bucketName") : null;
            String object = dest != null
                    ? (dest.getStringProperty("objectName") != null ? dest.getStringProperty("objectName") : dest.getKeyName())
                    : null;
            String endpoint = dest != null ? dest.getStringProperty("endpointOverride") : null;

            String side = tp.getType() != null ? tp.getType().name() : "UNKNOWN";

            int stateCode = tp.getState();
            String state = mapTransferState(stateCode);

            String errorDetail = tp.getErrorDetail();

            String counterPartyId = "unknown";
            var privateProps = tp.getPrivateProperties();
            if (privateProps != null) {
                Object cp = privateProps.get("edc:connectorId");
                if (cp == null) {
                    cp = privateProps.get("connectorId");
                }
                if (cp != null) {
                    counterPartyId = cp.toString();
                }
            }

            if (errorDetail != null) {
                AUDIT.info("AUDIT-TRANSFER: participant={} side={} phase={} eventId={} at={} transferId={} state={}({}) counterPartyId={} contractId={} consumerId={} providerId={} assetId={} destType={} bucket={} object={} endpoint={} error=\"{}\"",
                        participantId, side, phase, eventId, at, transferProcessId, state, stateCode, counterPartyId, contractId,
                        consumerId, providerId, assetId, destType, bucket, object, endpoint, errorDetail);
            } else {
                AUDIT.info("AUDIT-TRANSFER: participant={} side={} phase={} eventId={} at={} transferId={} state={}({}) counterPartyId={} contractId={} consumerId={} providerId={} assetId={} destType={} bucket={} object={} endpoint={}",
                        participantId, side, phase, eventId, at, transferProcessId, state, stateCode, counterPartyId, contractId,
                        consumerId, providerId, assetId, destType, bucket, object, endpoint);
            }
        }
    }
}
