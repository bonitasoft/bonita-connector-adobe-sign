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
class GetAgreementStatusConnectorTest {

    @Mock
    private AdobeSignClient mockClient;

    private GetAgreementStatusConnector connector;

    @BeforeEach
    void setUp() {
        connector = new GetAgreementStatusConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("authMode", "INTEGRATION_KEY");
        inputs.put("integrationKey", "test-key-123");
        inputs.put("region", "na1");
        inputs.put("agreementId", "agr-123");
        return inputs;
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractAdobeSignConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void should_execute_successfully_when_agreementId_provided() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getAgreementStatus("agr-123"))
                .thenReturn(new AdobeSignClient.GetAgreementStatusResult(
                        "SIGNED", "My Agreement", "2024-01-01T00:00:00Z", null));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("agreementStatus")).isEqualTo("SIGNED");
        assertThat(connector.getOutputs().get("agreementName")).isEqualTo("My Agreement");
        assertThat(connector.getOutputs().get("createdDate")).isEqualTo("2024-01-01T00:00:00Z");
    }

    @Test
    void should_fail_validation_when_agreementId_missing() {
        var inputs = validInputs();
        inputs.remove("agreementId");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("agreementId is mandatory");
    }

    @Test
    void should_fail_on_not_found() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getAgreementStatus("agr-123"))
                .thenThrow(new AdobeSignException("Agreement not found", 404, false));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(false);
        assertThat((String) connector.getOutputs().get("errorMessage"))
                .contains("Agreement not found");
    }

    @Test
    void should_populate_all_output_fields() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getAgreementStatus("agr-123"))
                .thenReturn(new AdobeSignClient.GetAgreementStatusResult(
                        "IN_PROCESS", "Test Agreement", "2024-06-15T10:30:00Z", "2024-07-15T10:30:00Z"));

        connector.executeBusinessLogic();

        var outputs = connector.getOutputs();
        assertThat(outputs.get("agreementStatus")).isEqualTo("IN_PROCESS");
        assertThat(outputs.get("agreementName")).isEqualTo("Test Agreement");
        assertThat(outputs.get("createdDate")).isEqualTo("2024-06-15T10:30:00Z");
        assertThat(outputs.get("expirationDate")).isEqualTo("2024-07-15T10:30:00Z");
        assertThat(outputs.get("success")).isEqualTo(true);
    }
}
