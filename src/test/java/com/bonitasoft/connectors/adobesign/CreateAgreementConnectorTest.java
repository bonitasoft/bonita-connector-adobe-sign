package com.bonitasoft.connectors.adobesign;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CreateAgreementConnectorTest {

    @Mock
    private AdobeSignClient mockClient;

    private CreateAgreementConnector connector;

    @BeforeEach
    void setUp() {
        connector = new CreateAgreementConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("authMode", "INTEGRATION_KEY");
        inputs.put("integrationKey", "test-key-123");
        inputs.put("region", "na1");
        inputs.put("documentBase64", "dGVzdA==");
        inputs.put("documentName", "test.pdf");
        inputs.put("agreementName", "Test Agreement");
        inputs.put("signerEmail", "signer@example.com");
        return inputs;
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractAdobeSignConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void should_execute_successfully_when_all_mandatory_inputs_provided() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createAgreement(
                anyString(), anyString(), anyString(), anyString(),
                isNull(), isNull(), isNull(), isNull(),
                eq("ESIGN"), isNull(), isNull(), eq("en_US"), eq("SENDER_SIGNS_LAST")))
                .thenReturn(new AdobeSignClient.CreateAgreementResult("agr-123", "IN_PROCESS", "td-456"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("agreementId")).isEqualTo("agr-123");
        assertThat(connector.getOutputs().get("agreementStatus")).isEqualTo("IN_PROCESS");
        assertThat(connector.getOutputs().get("transientDocumentId")).isEqualTo("td-456");
    }

    @Test
    void should_fail_validation_when_documentBase64_missing() {
        var inputs = validInputs();
        inputs.remove("documentBase64");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("documentBase64 is mandatory");
    }

    @Test
    void should_fail_validation_when_documentName_missing() {
        var inputs = validInputs();
        inputs.remove("documentName");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("documentName is mandatory");
    }

    @Test
    void should_fail_validation_when_agreementName_missing() {
        var inputs = validInputs();
        inputs.remove("agreementName");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("agreementName is mandatory");
    }

    @Test
    void should_fail_validation_when_signerEmail_missing() {
        var inputs = validInputs();
        inputs.remove("signerEmail");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("signerEmail is mandatory");
    }

    @Test
    void should_fail_validation_when_integrationKey_missing() {
        var inputs = validInputs();
        inputs.remove("integrationKey");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("integrationKey is mandatory");
    }

    @Test
    void should_set_error_outputs_on_auth_failure() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createAgreement(
                anyString(), anyString(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new AdobeSignException("Authentication failed: INVALID_ACCESS_TOKEN", 401, false));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(false);
        assertThat((String) connector.getOutputs().get("errorMessage"))
                .contains("Authentication failed");
    }

    @Test
    void should_set_error_outputs_on_unexpected_error() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createAgreement(
                anyString(), anyString(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(false);
        assertThat((String) connector.getOutputs().get("errorMessage"))
                .contains("Unexpected error");
    }

    @Test
    void should_apply_defaults_for_optional_inputs() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();

        // Verify configuration has defaults applied
        var configField = AbstractAdobeSignConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (AdobeSignConfiguration) configField.get(connector);

        assertThat(config.getSignatureType()).isEqualTo("ESIGN");
        assertThat(config.getLocale()).isEqualTo("en_US");
        assertThat(config.getSignatureFlow()).isEqualTo("SENDER_SIGNS_LAST");
        assertThat(config.getRegion()).isEqualTo("na1");
    }

    @Test
    void should_populate_all_output_fields_on_success() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createAgreement(
                anyString(), anyString(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new AdobeSignClient.CreateAgreementResult("agr-id", "IN_PROCESS", "td-id"));

        connector.executeBusinessLogic();

        var outputs = connector.getOutputs();
        assertThat(outputs.get("agreementId")).isNotNull();
        assertThat(outputs.get("agreementStatus")).isNotNull();
        assertThat(outputs.get("transientDocumentId")).isNotNull();
        assertThat(outputs.get("success")).isEqualTo(true);
    }
}
