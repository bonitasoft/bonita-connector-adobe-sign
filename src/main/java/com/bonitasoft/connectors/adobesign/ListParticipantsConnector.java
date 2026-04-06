package com.bonitasoft.connectors.adobesign;

import lombok.extern.slf4j.Slf4j;

/**
 * Lists participants of an Adobe Sign agreement.
 * API: GET /agreements/{agreementId}/members
 */
@Slf4j
public class ListParticipantsConnector extends AbstractAdobeSignConnector {

    static final String INPUT_AGREEMENT_ID = "agreementId";

    static final String OUTPUT_PARTICIPANT_SETS = "participantSets";
    static final String OUTPUT_SIGNER_COUNT = "signerCount";
    static final String OUTPUT_COMPLETED_SIGNER_COUNT = "completedSignerCount";
    static final String OUTPUT_ALL_SIGNERS_DONE = "allSignersDone";

    @Override
    protected AdobeSignConfiguration buildConfiguration() {
        return baseConfigurationBuilder()
                .agreementId(readStringInput(INPUT_AGREEMENT_ID))
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
        log.info("Executing ListParticipants connector for agreementId={}",
                configuration.getAgreementId());

        AdobeSignClient.ListParticipantsResult result =
                client.listParticipants(configuration.getAgreementId());

        setOutputParameter(OUTPUT_PARTICIPANT_SETS, result.participantSets());
        setOutputParameter(OUTPUT_SIGNER_COUNT, result.signerCount());
        setOutputParameter(OUTPUT_COMPLETED_SIGNER_COUNT, result.completedSignerCount());
        setOutputParameter(OUTPUT_ALL_SIGNERS_DONE, result.allSignersDone());

        log.info("ListParticipants executed successfully, signerCount={}, completedSignerCount={}",
                result.signerCount(), result.completedSignerCount());
    }
}
