package com.bonitasoft.connectors.adobesign;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * API client facade for Adobe Sign REST API v6.
 * Uses java.net.http.HttpClient and Jackson for JSON processing.
 */
@Slf4j
public class AdobeSignClient {

    private static final String API_PATH = "/api/rest/v6";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AdobeSignConfiguration configuration;
    private final RetryPolicy retryPolicy;
    private final HttpClient httpClient;
    private final String baseApiUrl;

    public AdobeSignClient(AdobeSignConfiguration configuration) throws AdobeSignException {
        this.configuration = configuration;
        this.retryPolicy = new RetryPolicy(configuration.getMaxRetries());
        this.baseApiUrl = configuration.getEffectiveBaseUrl() + API_PATH;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(configuration.getConnectTimeout()))
                .build();
        log.debug("AdobeSignClient initialized with base URL: {}", baseApiUrl);
    }

    // === CreateAgreement ===
    public CreateAgreementResult createAgreement(String documentBase64, String documentName,
            String agreementName, String signerEmail, String additionalParticipantSets,
            String ccEmails, String emailSubject, String message, String signatureType,
            Integer expirationDays, String reminderFrequency, String locale,
            String signatureFlow) throws AdobeSignException {
        return retryPolicy.execute(() -> {
            // Step 1: Upload transient document
            String transientDocumentId = uploadTransientDocument(documentBase64, documentName);

            // Step 2: Create the agreement
            ObjectNode body = MAPPER.createObjectNode();
            body.put("name", agreementName);
            body.put("signatureType", signatureType != null ? signatureType : "ESIGN");
            body.put("state", "IN_PROCESS");

            // File info
            ArrayNode fileInfos = body.putArray("fileInfos");
            ObjectNode fileInfo = fileInfos.addObject();
            fileInfo.put("transientDocumentId", transientDocumentId);

            // Participant sets
            ArrayNode participantSetsArr = body.putArray("participantSetsInfo");
            ObjectNode signerSet = participantSetsArr.addObject();
            signerSet.put("role", "SIGNER");
            signerSet.put("order", 1);
            ArrayNode memberInfos = signerSet.putArray("memberInfos");
            ObjectNode memberInfo = memberInfos.addObject();
            memberInfo.put("email", signerEmail);

            // Additional participant sets (JSON array string)
            if (additionalParticipantSets != null && !additionalParticipantSets.isBlank()) {
                try {
                    JsonNode additionalSets = MAPPER.readTree(additionalParticipantSets);
                    if (additionalSets.isArray()) {
                        for (JsonNode set : additionalSets) {
                            participantSetsArr.add(set);
                        }
                    }
                } catch (JsonProcessingException e) {
                    throw new AdobeSignException("Invalid JSON for additionalParticipantSets: " + e.getMessage());
                }
            }

            // CC emails
            if (ccEmails != null && !ccEmails.isBlank()) {
                ArrayNode ccs = body.putArray("ccs");
                for (String email : ccEmails.split("[,;\\n]")) {
                    String trimmed = email.trim();
                    if (!trimmed.isEmpty()) {
                        ObjectNode cc = ccs.addObject();
                        cc.put("email", trimmed);
                    }
                }
            }

            // Email configuration
            if (emailSubject != null || message != null) {
                ObjectNode emailOption = body.putObject("emailOption");
                ObjectNode sendOptions = emailOption.putObject("sendOptions");
                ObjectNode allOptions = sendOptions.putObject("completionEmails");
                allOptions.put("enabled", true);
                if (emailSubject != null) {
                    body.put("emailSubject", emailSubject);
                }
                if (message != null) {
                    body.put("message", message);
                }
            }

            // Expiration
            if (expirationDays != null && expirationDays > 0) {
                body.put("expirationTime", java.time.Instant.now()
                        .plus(java.time.Duration.ofDays(expirationDays)).toString());
            }

            // Reminder
            if (reminderFrequency != null && !reminderFrequency.isBlank()) {
                ObjectNode reminderConfig = body.putObject("reminderFrequency");
                reminderConfig.put("frequency", reminderFrequency);
            }

            // Locale
            if (locale != null && !locale.isBlank()) {
                body.put("locale", locale);
            }

            // Signature flow
            if (signatureFlow != null && !signatureFlow.isBlank()) {
                body.put("signatureFlow", signatureFlow);
            }

            HttpResponse<String> response = sendJson("POST", "/agreements", body.toString());
            JsonNode result = parseResponse(response);

            return new CreateAgreementResult(
                    result.path("id").asText(),
                    result.path("status").asText("IN_PROCESS"),
                    transientDocumentId);
        });
    }

    // === CreateFromTemplate ===
    public CreateFromTemplateResult createFromTemplate(String libraryDocumentId,
            String agreementName, String signerEmail, String mergeFieldInfo) throws AdobeSignException {
        return retryPolicy.execute(() -> {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("name", agreementName);
            body.put("signatureType", "ESIGN");
            body.put("state", "IN_PROCESS");

            // File info with library document
            ArrayNode fileInfos = body.putArray("fileInfos");
            ObjectNode fileInfo = fileInfos.addObject();
            fileInfo.put("libraryDocumentId", libraryDocumentId);

            // Participant sets
            ArrayNode participantSets = body.putArray("participantSetsInfo");
            ObjectNode signerSet = participantSets.addObject();
            signerSet.put("role", "SIGNER");
            signerSet.put("order", 1);
            ArrayNode memberInfos = signerSet.putArray("memberInfos");
            ObjectNode memberInfo = memberInfos.addObject();
            memberInfo.put("email", signerEmail);

            // Merge field info
            if (mergeFieldInfo != null && !mergeFieldInfo.isBlank()) {
                try {
                    JsonNode mergeFields = MAPPER.readTree(mergeFieldInfo);
                    body.set("mergeFieldInfo", mergeFields);
                } catch (JsonProcessingException e) {
                    throw new AdobeSignException("Invalid JSON for mergeFieldInfo: " + e.getMessage());
                }
            }

            HttpResponse<String> response = sendJson("POST", "/agreements", body.toString());
            JsonNode result = parseResponse(response);

            return new CreateFromTemplateResult(
                    result.path("id").asText(),
                    result.path("status").asText("IN_PROCESS"));
        });
    }

    // === GetAgreementStatus ===
    public GetAgreementStatusResult getAgreementStatus(String agreementId) throws AdobeSignException {
        return retryPolicy.execute(() -> {
            HttpResponse<String> response = sendGet("/agreements/" + agreementId);
            JsonNode result = parseResponse(response);

            return new GetAgreementStatusResult(
                    result.path("status").asText(),
                    result.path("name").asText(),
                    result.path("createdDate").asText(null),
                    result.path("expirationTime").asText(null));
        });
    }

    // === ListParticipants ===
    public ListParticipantsResult listParticipants(String agreementId) throws AdobeSignException {
        return retryPolicy.execute(() -> {
            HttpResponse<String> response = sendGet("/agreements/" + agreementId + "/members");
            JsonNode result = parseResponse(response);

            String participantSetsJson = MAPPER.writeValueAsString(result);
            JsonNode participantSetsNode = result.path("participantSets");
            int signerCount = 0;
            int completedSignerCount = 0;

            if (participantSetsNode.isArray()) {
                for (JsonNode setNode : participantSetsNode) {
                    if ("SIGNER".equals(setNode.path("role").asText())) {
                        JsonNode members = setNode.path("memberInfos");
                        if (members.isArray()) {
                            for (JsonNode member : members) {
                                signerCount++;
                                String status = member.path("status").asText("");
                                if ("COMPLETED".equals(status) || "SIGNED".equals(status)) {
                                    completedSignerCount++;
                                }
                            }
                        }
                    }
                }
            }

            return new ListParticipantsResult(
                    participantSetsJson, signerCount, completedSignerCount,
                    signerCount > 0 && signerCount == completedSignerCount);
        });
    }

    // === DownloadDocument ===
    public DownloadDocumentResult downloadDocument(String agreementId) throws AdobeSignException {
        return retryPolicy.execute(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseApiUrl + "/agreements/" + agreementId + "/combinedDocument"))
                    .header("Authorization", configuration.getAuthorizationHeader())
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                    .GET()
                    .build();

            HttpResponse<byte[]> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            } catch (IOException e) {
                throw new AdobeSignException("Network error downloading document: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AdobeSignException("Download interrupted", e);
            }

            if (response.statusCode() >= 400) {
                handleErrorResponse(response.statusCode(), new String(response.body()));
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("application/pdf");
            String contentDisposition = response.headers().firstValue("Content-Disposition").orElse("");
            String docName = extractFilenameFromDisposition(contentDisposition, "agreement.pdf");
            String base64Content = Base64.getEncoder().encodeToString(response.body());

            return new DownloadDocumentResult(
                    base64Content, docName, contentType, (long) response.body().length);
        });
    }

    // === CancelAgreement ===
    public CancelAgreementResult cancelAgreement(String agreementId, String comment,
            Boolean notifyParticipants) throws AdobeSignException {
        return retryPolicy.execute(() -> {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("state", "CANCELLED");
            if (comment != null && !comment.isBlank()) {
                ObjectNode agreementCancellationInfo = body.putObject("agreementCancellationInfo");
                agreementCancellationInfo.put("comment", comment);
                agreementCancellationInfo.put("notifyOthers",
                        notifyParticipants != null ? notifyParticipants : true);
            }

            try {
                HttpResponse<String> response = sendJson("PUT",
                        "/agreements/" + agreementId + "/state", body.toString());

                // 200 OK means success
                if (response.statusCode() == 200 || response.statusCode() == 204) {
                    return new CancelAgreementResult(agreementId, "CANCELLED");
                }

                // Handle AGREEMENT_ALREADY_CANCELLED as idempotent success
                if (response.statusCode() == 400) {
                    JsonNode errorNode = MAPPER.readTree(response.body());
                    String code = errorNode.path("code").asText("");
                    if ("AGREEMENT_ALREADY_CANCELLED".equals(code)) {
                        return new CancelAgreementResult(agreementId, "CANCELLED");
                    }
                }

                parseResponse(response); // Will throw for other errors
                return new CancelAgreementResult(agreementId, "CANCELLED");
            } catch (AdobeSignException e) {
                throw e;
            }
        });
    }

    // === Helper methods ===

    private String uploadTransientDocument(String documentBase64, String documentName)
            throws AdobeSignException {
        byte[] documentBytes = Base64.getDecoder().decode(documentBase64);

        String boundary = "----FormBoundary" + System.currentTimeMillis();
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"File-Name\"\r\n\r\n"
                + documentName + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"Mime-Type\"\r\n\r\n"
                + "application/pdf\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"File\"; filename=\"" + documentName + "\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";

        byte[] headerBytes = body.getBytes();
        byte[] tailBytes = tail.getBytes();
        byte[] fullBody = new byte[headerBytes.length + documentBytes.length + tailBytes.length];
        System.arraycopy(headerBytes, 0, fullBody, 0, headerBytes.length);
        System.arraycopy(documentBytes, 0, fullBody, headerBytes.length, documentBytes.length);
        System.arraycopy(tailBytes, 0, fullBody, headerBytes.length + documentBytes.length, tailBytes.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseApiUrl + "/transientDocuments"))
                .header("Authorization", configuration.getAuthorizationHeader())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                .POST(HttpRequest.BodyPublishers.ofByteArray(fullBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AdobeSignException("Network error uploading document: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AdobeSignException("Upload interrupted", e);
        }

        JsonNode result = parseResponse(response);
        return result.path("transientDocumentId").asText();
    }

    private HttpResponse<String> sendJson(String method, String path, String jsonBody)
            throws AdobeSignException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseApiUrl + path))
                .header("Authorization", configuration.getAuthorizationHeader())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(configuration.getReadTimeout()));

        if ("PUT".equals(method)) {
            builder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        }

        try {
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AdobeSignException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AdobeSignException("Request interrupted", e);
        }
    }

    private HttpResponse<String> sendGet(String path) throws AdobeSignException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseApiUrl + path))
                .header("Authorization", configuration.getAuthorizationHeader())
                .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                .GET()
                .build();

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AdobeSignException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AdobeSignException("Request interrupted", e);
        }
    }

    private JsonNode parseResponse(HttpResponse<String> response) throws AdobeSignException {
        if (response.statusCode() >= 400) {
            handleErrorResponse(response.statusCode(), response.body());
        }
        try {
            return MAPPER.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new AdobeSignException("Failed to parse API response: " + e.getMessage(), e);
        }
    }

    private void handleErrorResponse(int statusCode, String body) throws AdobeSignException {
        String errorMessage;
        try {
            JsonNode errorNode = MAPPER.readTree(body);
            String code = errorNode.path("code").asText("");
            String msg = errorNode.path("message").asText(body);
            errorMessage = code.isEmpty() ? msg : code + ": " + msg;
        } catch (JsonProcessingException e) {
            errorMessage = "HTTP " + statusCode + ": " + body;
        }

        boolean retryable = RetryPolicy.isRetryableStatusCode(statusCode);

        switch (statusCode) {
            case 400 -> throw new AdobeSignException("Bad request: " + errorMessage, statusCode, false);
            case 401 -> throw new AdobeSignException("Authentication failed: " + errorMessage, statusCode, false);
            case 403 -> throw new AdobeSignException("Permission denied: " + errorMessage, statusCode, false);
            case 404 -> throw new AdobeSignException("Agreement not found", statusCode, false);
            case 429 -> throw new AdobeSignException("Rate limit exceeded: " + errorMessage, statusCode, true);
            default -> throw new AdobeSignException(errorMessage, statusCode, retryable);
        }
    }

    private String extractFilenameFromDisposition(String disposition, String defaultName) {
        if (disposition != null && disposition.contains("filename=")) {
            String[] parts = disposition.split("filename=");
            if (parts.length > 1) {
                return parts[1].replaceAll("\"", "").trim();
            }
        }
        return defaultName;
    }

    // === Result records ===

    public record CreateAgreementResult(String agreementId, String agreementStatus,
            String transientDocumentId) {}

    public record CreateFromTemplateResult(String agreementId, String agreementStatus) {}

    public record GetAgreementStatusResult(String agreementStatus, String agreementName,
            String createdDate, String expirationDate) {}

    public record ListParticipantsResult(String participantSets, int signerCount,
            int completedSignerCount, boolean allSignersDone) {}

    public record DownloadDocumentResult(String documentContent, String documentName,
            String contentType, Long contentLength) {}

    public record CancelAgreementResult(String agreementId, String agreementStatus) {}
}
