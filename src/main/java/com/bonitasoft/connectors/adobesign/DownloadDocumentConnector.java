package com.bonitasoft.connectors.adobesign;

import lombok.extern.slf4j.Slf4j;

/**
 * Downloads the combined document of an Adobe Sign agreement.
 * API: GET /agreements/{agreementId}/combinedDocument
 */
@Slf4j
public class DownloadDocumentConnector extends AbstractAdobeSignConnector {

    static final String INPUT_AGREEMENT_ID = "agreementId";

    static final String OUTPUT_DOCUMENT_CONTENT = "documentContent";
    static final String OUTPUT_DOCUMENT_NAME = "documentName";
    static final String OUTPUT_CONTENT_TYPE = "contentType";
    static final String OUTPUT_CONTENT_LENGTH = "contentLength";

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
        log.info("Executing DownloadDocument connector for agreementId={}",
                configuration.getAgreementId());

        AdobeSignClient.DownloadDocumentResult result =
                client.downloadDocument(configuration.getAgreementId());

        setOutputParameter(OUTPUT_DOCUMENT_CONTENT, result.documentContent());
        setOutputParameter(OUTPUT_DOCUMENT_NAME, result.documentName());
        setOutputParameter(OUTPUT_CONTENT_TYPE, result.contentType());
        setOutputParameter(OUTPUT_CONTENT_LENGTH, result.contentLength());

        log.info("DownloadDocument executed successfully, documentName={}, contentLength={}",
                result.documentName(), result.contentLength());
    }
}
