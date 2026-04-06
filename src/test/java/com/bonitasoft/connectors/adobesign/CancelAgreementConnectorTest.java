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
class CancelAgreementConnectorTest {

    @Mock
    private AdobeSignClient mockClient;

    private CancelAgreementConnector connector;

    @BeforeEach
    void setUp() {
        connector = new CancelAgreementConnector();
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
    void should_cancel_successfully_when_agreementId_provided() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.cancelAgreement(eq("agr-123"), isNull(), eq(true)))
                .thenReturn(new AdobeSignClient.CancelAgreementResult("agr-123", "CANCELLED"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("agreementId")).isEqualTo("agr-123");
        assertThat(connector.getOutputs().get("agreementStatus")).isEqualTo("CANCELLED");
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
    void should_pass_comment_and_notify_flag() throws Exception {
        var inputs = validInputs();
        inputs.put("comment", "No longer needed");
        inputs.put("notifyParticipants", false);
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.cancelAgreement("agr-123", "No longer needed", false))
                .thenReturn(new AdobeSignClient.CancelAgreementResult("agr-123", "CANCELLED"));

        connector.executeBusinessLogic();

        verify(mockClient).cancelAgreement("agr-123", "No longer needed", false);
        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
    }

    @Test
    void should_default_notify_participants_to_true() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();

        var configField = AbstractAdobeSignConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (AdobeSignConfiguration) configField.get(connector);

        assertThat(config.getNotifyParticipants()).isTrue();
    }

    @Test
    void should_set_error_outputs_on_failure() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.cancelAgreement(eq("agr-123"), any(), any()))
                .thenThrow(new AdobeSignException("Agreement not found", 404, false));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(false);
        assertThat((String) connector.getOutputs().get("errorMessage"))
                .contains("Agreement not found");
    }
}
