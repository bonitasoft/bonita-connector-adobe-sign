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
class ListParticipantsConnectorTest {

    @Mock
    private AdobeSignClient mockClient;

    private ListParticipantsConnector connector;

    @BeforeEach
    void setUp() {
        connector = new ListParticipantsConnector();
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

        when(mockClient.listParticipants("agr-123"))
                .thenReturn(new AdobeSignClient.ListParticipantsResult(
                        "{\"participantSets\":[]}", 2, 1, false));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("signerCount")).isEqualTo(2);
        assertThat(connector.getOutputs().get("completedSignerCount")).isEqualTo(1);
        assertThat(connector.getOutputs().get("allSignersDone")).isEqualTo(false);
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
    void should_report_all_signers_done_when_all_completed() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.listParticipants("agr-123"))
                .thenReturn(new AdobeSignClient.ListParticipantsResult(
                        "{}", 3, 3, true));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("allSignersDone")).isEqualTo(true);
        assertThat(connector.getOutputs().get("signerCount")).isEqualTo(3);
        assertThat(connector.getOutputs().get("completedSignerCount")).isEqualTo(3);
    }

    @Test
    void should_set_error_outputs_on_failure() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.listParticipants("agr-123"))
                .thenThrow(new AdobeSignException("Permission denied", 403, false));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(false);
        assertThat((String) connector.getOutputs().get("errorMessage"))
                .contains("Permission denied");
    }
}
