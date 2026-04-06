package com.bonitasoft.connectors.adobesign;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates an Adobe Sign agreement by uploading a document and sending it for signature.
 * API: POST /transientDocuments then POST /agreements
 */
@Slf4j
public class CreateAgreementConnector extends AbstractAdobeSignConnector {

    // Input parameter name constants
    static final String INPUT_DOCUMENT_BASE64 = "documentBase64";
    static final String INPUT_DOCUMENT_NAME = "documentName";
    static final String INPUT_AGREEMENT_NAME = "agreementName";
    static final String INPUT_SIGNER_EMAIL = "signerEmail";
    static final String INPUT_ADDITIONAL_PARTICIPANT_SETS = "additionalParticipantSets";
    static final String INPUT_CC_EMAILS = "ccEmails";
    static final String INPUT_EMAIL_SUBJECT = "emailSubject";
    static final String INPUT_MESSAGE = "message";
    static final String INPUT_SIGNATURE_TYPE = "signatureType";
    static final String INPUT_EXPIRATION_DAYS = "expirationDays";
    static final String INPUT_REMINDER_FREQUENCY = "reminderFrequency";
    static final String INPUT_LOCALE = "locale";
    static final String INPUT_SIGNATURE_FLOW = "signatureFlow";

    // Output parameter name constants
    static final String OUTPUT_AGREEMENT_ID = "agreementId";
    static final String OUTPUT_AGREEMENT_STATUS = "agreementStatus";
    static final String OUTPUT_TRANSIENT_DOCUMENT_ID = "transientDocumentId";

    @Override
    protected AdobeSignConfiguration buildConfiguration() {
        return baseConfigurationBuilder()
                .documentBase64(readStringInput(INPUT_DOCUMENT_BASE64))
                .documentName(readStringInput(INPUT_DOCUMENT_NAME))
                .agreementName(readStringInput(INPUT_AGREEMENT_NAME))
                .signerEmail(readStringInput(INPUT_SIGNER_EMAIL))
                .additionalParticipantSets(readStringInput(INPUT_ADDITIONAL_PARTICIPANT_SETS))
                .ccEmails(readStringInput(INPUT_CC_EMAILS))
                .emailSubject(readStringInput(INPUT_EMAIL_SUBJECT))
                .message(readStringInput(INPUT_MESSAGE))
                .signatureType(readStringInput(INPUT_SIGNATURE_TYPE, "ESIGN"))
                .expirationDays(getInputParameter(INPUT_EXPIRATION_DAYS) != null
                        ? readIntegerInput(INPUT_EXPIRATION_DAYS, 0) : null)
                .reminderFrequency(readStringInput(INPUT_REMINDER_FREQUENCY))
                .locale(readStringInput(INPUT_LOCALE, "en_US"))
                .signatureFlow(readStringInput(INPUT_SIGNATURE_FLOW, "SENDER_SIGNS_LAST"))
                .build();
    }

    @Override
    protected void validateConfiguration(AdobeSignConfiguration config) {
        super.validateConfiguration(config);
        if (config.getDocumentBase64() == null || config.getDocumentBase64().isBlank()) {
            throw new IllegalArgumentException("documentBase64 is mandatory");
        }
        if (config.getDocumentName() == null || config.getDocumentName().isBlank()) {
            throw new IllegalArgumentException("documentName is mandatory");
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
        log.info("Executing CreateAgreement connector");

        AdobeSignClient.CreateAgreementResult result = client.createAgreement(
                configuration.getDocumentBase64(),
                configuration.getDocumentName(),
                configuration.getAgreementName(),
                configuration.getSignerEmail(),
                configuration.getAdditionalParticipantSets(),
                configuration.getCcEmails(),
                configuration.getEmailSubject(),
                configuration.getMessage(),
                configuration.getSignatureType(),
                configuration.getExpirationDays(),
                configuration.getReminderFrequency(),
                configuration.getLocale(),
                configuration.getSignatureFlow());

        setOutputParameter(OUTPUT_AGREEMENT_ID, result.agreementId());
        setOutputParameter(OUTPUT_AGREEMENT_STATUS, result.agreementStatus());
        setOutputParameter(OUTPUT_TRANSIENT_DOCUMENT_ID, result.transientDocumentId());

        log.info("CreateAgreement connector executed successfully, agreementId={}", result.agreementId());
    }
}
