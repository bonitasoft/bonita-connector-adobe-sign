package com.bonitasoft.connectors.adobesign;

import lombok.extern.slf4j.Slf4j;

/**
 * Retrieves the status of an Adobe Sign agreement.
 * API: GET /agreements/{agreementId}
 */
@Slf4j
public class GetAgreementStatusConnector extends AbstractAdobeSignConnector {

    static final String INPUT_AGREEMENT_ID = "agreementId";

    static final String OUTPUT_AGREEMENT_STATUS = "agreementStatus";
    static final String OUTPUT_AGREEMENT_NAME = "agreementName";
    static final String OUTPUT_CREATED_DATE = "createdDate";
    static final String OUTPUT_EXPIRATION_DATE = "expirationDate";

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
        log.info("Executing GetAgreementStatus connector for agreementId={}",
                configuration.getAgreementId());

        AdobeSignClient.GetAgreementStatusResult result =
                client.getAgreementStatus(configuration.getAgreementId());

        setOutputParameter(OUTPUT_AGREEMENT_STATUS, result.agreementStatus());
        setOutputParameter(OUTPUT_AGREEMENT_NAME, result.agreementName());
        setOutputParameter(OUTPUT_CREATED_DATE, result.createdDate());
        setOutputParameter(OUTPUT_EXPIRATION_DATE, result.expirationDate());

        log.info("GetAgreementStatus executed successfully, status={}", result.agreementStatus());
    }
}
