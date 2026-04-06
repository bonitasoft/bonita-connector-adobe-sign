package com.bonitasoft.connectors.adobesign;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates an Adobe Sign agreement from a library template.
 * API: POST /agreements (with libraryDocumentId)
 */
@Slf4j
public class CreateFromTemplateConnector extends AbstractAdobeSignConnector {

    static final String INPUT_LIBRARY_DOCUMENT_ID = "libraryDocumentId";
    static final String INPUT_AGREEMENT_NAME = "agreementName";
    static final String INPUT_SIGNER_EMAIL = "signerEmail";
    static final String INPUT_MERGE_FIELD_INFO = "mergeFieldInfo";

    static final String OUTPUT_AGREEMENT_ID = "agreementId";
    static final String OUTPUT_AGREEMENT_STATUS = "agreementStatus";

    @Override
    protected AdobeSignConfiguration buildConfiguration() {
        return baseConfigurationBuilder()
                .libraryDocumentId(readStringInput(INPUT_LIBRARY_DOCUMENT_ID))
                .agreementName(readStringInput(INPUT_AGREEMENT_NAME))
                .signerEmail(readStringInput(INPUT_SIGNER_EMAIL))
                .mergeFieldInfo(readStringInput(INPUT_MERGE_FIELD_INFO))
                .build();
    }

    @Override
    protected void validateConfiguration(AdobeSignConfiguration config) {
        super.validateConfiguration(config);
        if (config.getLibraryDocumentId() == null || config.getLibraryDocumentId().isBlank()) {
            throw new IllegalArgumentException("libraryDocumentId is mandatory");
        }
        if (config.getAgreementName() == null || config.getAgreementName().isBlank()) {
            throw new IllegalArgumentException("agreementName is mandatory");
        }
        if (config.getSignerEmail() == null || config.getSignerEmail().isBlank()) {
            throw new IllegalArgumentException("signerEmail is mandatory");
        }
    }

    @Override
    protected void doExecute() throws AdobeSignException {
        log.info("Executing CreateFromTemplate connector");

        AdobeSignClient.CreateFromTemplateResult result = client.createFromTemplate(
                configuration.getLibraryDocumentId(),
                configuration.getAgreementName(),
                configuration.getSignerEmail(),
                configuration.getMergeFieldInfo());

        setOutputParameter(OUTPUT_AGREEMENT_ID, result.agreementId());
        setOutputParameter(OUTPUT_AGREEMENT_STATUS, result.agreementStatus());

        log.info("CreateFromTemplate connector executed successfully, agreementId={}", result.agreementId());
    }
}
