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
class CreateFromTemplateConnectorTest {

    @Mock
    private AdobeSignClient mockClient;

    private CreateFromTemplateConnector connector;

    @BeforeEach
    void setUp() {
        connector = new CreateFromTemplateConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("authMode", "INTEGRATION_KEY");
        inputs.put("integrationKey", "test-key-123");
        inputs.put("region", "na1");
        inputs.put("libraryDocumentId", "lib-doc-123");
        inputs.put("agreementName", "Template Agreement");
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

        when(mockClient.createFromTemplate(anyString(), anyString(), anyString(), isNull()))
                .thenReturn(new AdobeSignClient.CreateFromTemplateResult("agr-789", "IN_PROCESS"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("agreementId")).isEqualTo("agr-789");
        assertThat(connector.getOutputs().get("agreementStatus")).isEqualTo("IN_PROCESS");
    }

    @Test
    void should_fail_validation_when_libraryDocumentId_missing() {
        var inputs = validInputs();
        inputs.remove("libraryDocumentId");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("libraryDocumentId is mandatory");
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
    void should_set_error_outputs_on_failure() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createFromTemplate(anyString(), anyString(), anyString(), any()))
                .thenThrow(new AdobeSignException("Bad request: INVALID_ARGUMENTS", 400, false));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(false);
        assertThat((String) connector.getOutputs().get("errorMessage"))
                .contains("Bad request");
    }

    @Test
    void should_pass_merge_field_info_when_provided() throws Exception {
        var inputs = validInputs();
        inputs.put("mergeFieldInfo", "[{\"fieldName\":\"company\",\"defaultValue\":\"Acme\"}]");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createFromTemplate(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new AdobeSignClient.CreateFromTemplateResult("agr-merge", "IN_PROCESS"));

        connector.executeBusinessLogic();

        verify(mockClient).createFromTemplate(
                eq("lib-doc-123"), eq("Template Agreement"), eq("signer@example.com"),
                eq("[{\"fieldName\":\"company\",\"defaultValue\":\"Acme\"}]"));
        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
    }
}
