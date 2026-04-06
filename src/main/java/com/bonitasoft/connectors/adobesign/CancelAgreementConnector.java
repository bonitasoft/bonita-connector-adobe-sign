package com.bonitasoft.connectors.adobesign;

import lombok.extern.slf4j.Slf4j;

/**
 * Cancels an Adobe Sign agreement.
 * API: PUT /agreements/{agreementId}/state
 */
@Slf4j
public class CancelAgreementConnector extends AbstractAdobeSignConnector {

    static final String INPUT_AGREEMENT_ID = "agreementId";
    static final String INPUT_COMMENT = "comment";
    static final String INPUT_NOTIFY_PARTICIPANTS = "notifyParticipants";

    static final String OUTPUT_AGREEMENT_ID = "agreementId";
    static final String OUTPUT_AGREEMENT_STATUS = "agreementStatus";

    @Override
    protected AdobeSignConfiguration buildConfiguration() {
        return baseConfigurationBuilder()
                .agreementId(readStringInput(INPUT_AGREEMENT_ID))
                .comment(readStringInput(INPUT_COMMENT))
                .notifyParticipants(readBooleanInput(INPUT_NOTIFY_PARTICIPANTS, true))
                .build();
    }

    @Override
    protected void validateConfiguration(AdobeSignConfiguration config) {
        super.validateConfiguration(config);
        if (config.getAgreementId() == null || config.getAgreementId().isBlank()) {
            throw new IllegalArgumentException("agreementId is mandatory");
        }
    }

    @Override
    protected void doExecute() throws AdobeSignException {
        log.info("Executing CancelAgreement connector for agreementId={}",
                configuration.getAgreementId());

        AdobeSignClient.CancelAgreementResult result = client.cancelAgreement(
                configuration.getAgreementId(),
                configuration.getComment(),
                configuration.getNotifyParticipants());

        setOutputParameter(OUTPUT_AGREEMENT_ID, result.agreementId());
        setOutputParameter(OUTPUT_AGREEMENT_STATUS, result.agreementStatus());

        log.info("CancelAgreement executed successfully, status={}", result.agreementStatus());
    }
}
