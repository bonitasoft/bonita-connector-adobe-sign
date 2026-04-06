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
class DownloadDocumentConnectorTest {

    @Mock
    private AdobeSignClient mockClient;

    private DownloadDocumentConnector connector;

    @BeforeEach
    void setUp() {
        connector = new DownloadDocumentConnector();
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

        when(mockClient.downloadDocument("agr-123"))
                .thenReturn(new AdobeSignClient.DownloadDocumentResult(
                        "dGVzdA==", "signed-agreement.pdf", "application/pdf", 4L));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("documentContent")).isEqualTo("dGVzdA==");
        assertThat(connector.getOutputs().get("documentName")).isEqualTo("signed-agreement.pdf");
        assertThat(connector.getOutputs().get("contentType")).isEqualTo("application/pdf");
        assertThat(connector.getOutputs().get("contentLength")).isEqualTo(4L);
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
    void should_set_error_outputs_on_network_timeout() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.downloadDocument("agr-123"))
                .thenThrow(new AdobeSignException("Network error downloading document: Read timed out"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(false);
        assertThat((String) connector.getOutputs().get("errorMessage"))
                .contains("Network error");
    }

    @Test
    void should_populate_all_output_fields() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.downloadDocument("agr-123"))
                .thenReturn(new AdobeSignClient.DownloadDocumentResult(
                        "Y29udGVudA==", "contract.pdf", "application/pdf", 1024L));

        connector.executeBusinessLogic();

        var outputs = connector.getOutputs();
        assertThat(outputs).containsKeys("documentContent", "documentName", "contentType", "contentLength", "success");
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("documentContent")).isNotNull();
        assertThat(outputs.get("documentName")).isNotNull();
    }
}
