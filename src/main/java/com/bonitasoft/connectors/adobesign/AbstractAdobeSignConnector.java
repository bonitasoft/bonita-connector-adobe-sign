package com.bonitasoft.connectors.adobesign;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

/**
 * Abstract base connector for Adobe Sign.
 * Handles connection lifecycle, validation, and error handling.
 */
@Slf4j
public abstract class AbstractAdobeSignConnector extends AbstractConnector {

    // Connection parameter constants
    static final String INPUT_AUTH_MODE = "authMode";
    static final String INPUT_INTEGRATION_KEY = "integrationKey";
    static final String INPUT_CLIENT_ID = "clientId";
    static final String INPUT_CLIENT_SECRET = "clientSecret";
    static final String INPUT_REFRESH_TOKEN = "refreshToken";
    static final String INPUT_REGION = "region";
    static final String INPUT_BASE_URL = "baseUrl";
    static final String INPUT_CONNECT_TIMEOUT = "connectTimeout";
    static final String INPUT_READ_TIMEOUT = "readTimeout";

    // Output parameter constants
    protected static final String OUTPUT_SUCCESS = "success";
    protected static final String OUTPUT_ERROR_MESSAGE = "errorMessage";

    protected AdobeSignConfiguration configuration;
    protected AdobeSignClient client;

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        try {
            this.configuration = buildConfiguration();
            validateConfiguration(this.configuration);
        } catch (IllegalArgumentException e) {
            throw new ConnectorValidationException(this, e.getMessage());
        }
    }

    @Override
    public void connect() throws ConnectorException {
        try {
            this.client = new AdobeSignClient(this.configuration);
            log.info("Adobe Sign connector connected successfully");
        } catch (AdobeSignException e) {
            throw new ConnectorException("Failed to connect: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() throws ConnectorException {
        this.client = null;
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            doExecute();
            setOutputParameter(OUTPUT_SUCCESS, true);
        } catch (AdobeSignException e) {
            log.error("Adobe Sign connector execution failed: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in Adobe Sign connector: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, "Unexpected error: " + e.getMessage());
        }
    }

    protected abstract void doExecute() throws AdobeSignException;

    protected abstract AdobeSignConfiguration buildConfiguration();

    /**
     * Validates the configuration. Checks auth mode and required credentials.
     */
    protected void validateConfiguration(AdobeSignConfiguration config) {
        String authMode = config.getAuthMode();
        if (authMode == null || authMode.isBlank()) {
            throw new IllegalArgumentException("authMode is mandatory");
        }

        if ("INTEGRATION_KEY".equals(authMode)) {
            if (config.getIntegrationKey() == null || config.getIntegrationKey().isBlank()) {
                throw new IllegalArgumentException("integrationKey is mandatory when authMode is INTEGRATION_KEY");
            }
        } else if ("OAUTH2".equals(authMode)) {
            if (config.getClientId() == null || config.getClientId().isBlank()) {
                throw new IllegalArgumentException("clientId is mandatory when authMode is OAUTH2");
            }
            if (config.getClientSecret() == null || config.getClientSecret().isBlank()) {
                throw new IllegalArgumentException("clientSecret is mandatory when authMode is OAUTH2");
            }
            if (config.getRefreshToken() == null || config.getRefreshToken().isBlank()) {
                throw new IllegalArgumentException("refreshToken is mandatory when authMode is OAUTH2");
            }
        } else {
            throw new IllegalArgumentException("Invalid authMode: " + authMode
                    + ". Must be INTEGRATION_KEY or OAUTH2");
        }
    }

    /** Helper: read a String input, returning null if not set. */
    protected String readStringInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? value.toString() : null;
    }

    /** Helper: read a String input with a default value. */
    protected String readStringInput(String name, String defaultValue) {
        String value = readStringInput(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /** Helper: read a Boolean input with a default value. */
    protected Boolean readBooleanInput(String name, boolean defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? (Boolean) value : defaultValue;
    }

    /**
     * Exposes output parameters for testing. Package-visible.
     */
    Map<String, Object> getOutputs() {
        return getOutputParameters();
    }

    /** Helper: read an Integer input with a default value. */
    protected Integer readIntegerInput(String name, int defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).intValue() : defaultValue;
    }

    /**
     * Builds the common connection configuration from shared input parameters.
     * Subclasses call this in their buildConfiguration() to get a pre-filled builder.
     */
    protected AdobeSignConfiguration.AdobeSignConfigurationBuilder baseConfigurationBuilder() {
        return AdobeSignConfiguration.builder()
                .authMode(readStringInput(INPUT_AUTH_MODE, "INTEGRATION_KEY"))
                .integrationKey(readStringInput(INPUT_INTEGRATION_KEY))
                .clientId(readStringInput(INPUT_CLIENT_ID))
                .clientSecret(readStringInput(INPUT_CLIENT_SECRET))
                .refreshToken(readStringInput(INPUT_REFRESH_TOKEN))
                .region(readStringInput(INPUT_REGION, "na1"))
                .baseUrl(readStringInput(INPUT_BASE_URL))
                .connectTimeout(readIntegerInput(INPUT_CONNECT_TIMEOUT, 30000))
                .readTimeout(readIntegerInput(INPUT_READ_TIMEOUT, 60000));
    }
}
