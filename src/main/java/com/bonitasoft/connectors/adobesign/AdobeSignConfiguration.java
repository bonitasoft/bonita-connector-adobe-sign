package com.bonitasoft.connectors.adobesign;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for Adobe Sign connector.
 * Holds all connection, auth, and operation parameters.
 */
@Data
@Builder
public class AdobeSignConfiguration {

    // === Connection / Auth parameters ===
    @Builder.Default
    private String authMode = "INTEGRATION_KEY";

    private String integrationKey;
    private String clientId;
    private String clientSecret;
    private String refreshToken;

    @Builder.Default
    private String region = "na1";

    private String baseUrl;

    @Builder.Default
    private int connectTimeout = 30000;

    @Builder.Default
    private int readTimeout = 60000;

    // === Operation parameters ===
    // CreateAgreement
    private String documentBase64;
    private String documentName;
    private String agreementName;
    private String signerEmail;
    private String additionalParticipantSets;
    private String ccEmails;
    private String emailSubject;
    private String message;
    @Builder.Default
    private String signatureType = "ESIGN";
    private Integer expirationDays;
    private String reminderFrequency;
    @Builder.Default
    private String locale = "en_US";
    @Builder.Default
    private String signatureFlow = "SENDER_SIGNS_LAST";

    // CreateFromTemplate
    private String libraryDocumentId;
    private String mergeFieldInfo;

    // GetAgreementStatus / DownloadDocument / ListParticipants / CancelAgreement
    private String agreementId;

    // CancelAgreement
    private String comment;
    @Builder.Default
    private Boolean notifyParticipants = true;

    // === Advanced parameters ===
    @Builder.Default
    private int maxRetries = 3;

    /**
     * Resolves the effective base URL from region or explicit baseUrl override.
     */
    public String getEffectiveBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        }
        return switch (region) {
            case "na1" -> "https://api.na1.adobesign.com";
            case "na2" -> "https://api.na2.adobesign.com";
            case "eu1" -> "https://api.eu1.adobesign.com";
            case "eu2" -> "https://api.eu2.adobesign.com";
            case "jp1" -> "https://api.jp1.adobesign.com";
            case "au1" -> "https://api.au1.adobesign.com";
            default -> "https://api.na1.adobesign.com";
        };
    }

    /**
     * Returns the authorization header value based on auth mode.
     */
    public String getAuthorizationHeader() {
        return "Bearer " + integrationKey;
    }
}
