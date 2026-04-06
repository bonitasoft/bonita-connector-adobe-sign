package com.bonitasoft.connectors.adobesign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.bonitasoft.web.client.BonitaClient;
import org.bonitasoft.web.client.api.ArchivedProcessInstanceApi;
import org.bonitasoft.web.client.api.ProcessInstanceApi;
import org.bonitasoft.web.client.exception.NotFoundException;
import org.bonitasoft.web.client.model.ArchivedProcessInstance;
import org.bonitasoft.web.client.services.policies.OrganizationImportPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Process-based integration tests for Adobe Sign connectors.
 *
 * These tests build a Bonita process containing the connector, deploy it
 * to a Docker Bonita instance, and verify the connector executes correctly
 * within the process engine.
 *
 * Requires:
 * - Docker running
 * - Environment variable ADOBESIGN_INTEGRATION_KEY set
 * - Project built with mvn package (JAR must exist in target/)
 */
@Testcontainers
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "ADOBESIGN_INTEGRATION_KEY", matches = ".+")
class AdobeSignConnectorIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdobeSignConnectorIT.class);

    // Connector definition IDs and versions (must match pom.xml properties)
    private static final String CREATE_AGREEMENT_DEF_ID =
            "com.bonitasoft.connectors.adobesign.CreateAgreementDefinition";
    private static final String CREATE_AGREEMENT_DEF_VERSION = "1.0.0";

    private static final String GET_AGREEMENT_STATUS_DEF_ID =
            "com.bonitasoft.connectors.adobesign.GetAgreementStatusDefinition";
    private static final String GET_AGREEMENT_STATUS_DEF_VERSION = "1.0.0";

    private static final String CANCEL_AGREEMENT_DEF_ID =
            "com.bonitasoft.connectors.adobesign.CancelAgreementDefinition";
    private static final String CANCEL_AGREEMENT_DEF_VERSION = "1.0.0";

    @Container
    static GenericContainer<?> BONITA_CONTAINER = new GenericContainer<>(
            DockerImageName.parse("bonita:10.2.0"))
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/bonita"))
            .withLogConsumer(new Slf4jLogConsumer(LOGGER));

    private BonitaClient client;

    @BeforeAll
    static void installOrganization() {
        var client = BonitaClient
                .builder(String.format("http://%s:%s/bonita",
                        BONITA_CONTAINER.getHost(),
                        BONITA_CONTAINER.getFirstMappedPort()))
                .build();
        client.login("install", "install");
        client.users().importOrganization(
                new File(AdobeSignConnectorIT.class.getResource("/ACME.xml").getFile()),
                OrganizationImportPolicy.IGNORE_DUPLICATES);
        client.logout();
    }

    @BeforeEach
    void login() {
        client = BonitaClient
                .builder(String.format("http://%s:%s/bonita",
                        BONITA_CONTAINER.getHost(),
                        BONITA_CONTAINER.getFirstMappedPort()))
                .build();
        client.login("install", "install");
    }

    @AfterEach
    void logout() {
        client.logout();
    }

    // TODO: Add integration tests per operation when ADOBESIGN_INTEGRATION_KEY is available
    // Example:
    //
    // @Test
    // void testGetAgreementStatusConnector() throws Exception {
    //     var inputs = commonInputs();
    //     inputs.put("agreementId", System.getenv("ADOBESIGN_TEST_AGREEMENT_ID"));
    //
    //     var outputs = Map.of(
    //             "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
    //             "resultStatus", ConnectorTestToolkit.Output.create("agreementStatus", String.class.getName()));
    //
    //     var barFile = ConnectorTestToolkit.buildConnectorToTest(
    //             GET_AGREEMENT_STATUS_DEF_ID, GET_AGREEMENT_STATUS_DEF_VERSION, inputs, outputs);
    //     var processResponse = ConnectorTestToolkit.importAndLaunchProcess(barFile, client);
    //
    //     await().until(pollInstanceState(processResponse.getCaseId()), "started"::equals);
    //
    //     var success = ConnectorTestToolkit.getProcessVariableValue(client,
    //             processResponse.getCaseId(), "resultSuccess");
    //     assertThat(success).isEqualTo("true");
    // }

    /**
     * Common inputs shared across all connector operations.
     */
    private Map<String, String> commonInputs() {
        var inputs = new HashMap<String, String>();
        inputs.put("authMode", "INTEGRATION_KEY");
        inputs.put("integrationKey", System.getenv("ADOBESIGN_INTEGRATION_KEY"));
        inputs.put("region", "na1");
        return inputs;
    }

    private Callable<String> pollInstanceState(String id) {
        return () -> {
            try {
                var instance = client.get(ProcessInstanceApi.class)
                        .getProcessInstanceById(id, (String) null);
                return instance.getState().name().toLowerCase();
            } catch (NotFoundException e) {
                var archived = getCompletedProcess(id);
                return archived != null ? archived.getState().name().toLowerCase() : "unknown";
            }
        };
    }

    private ArchivedProcessInstance getCompletedProcess(String id) {
        var archivedInstances = client.get(ArchivedProcessInstanceApi.class)
                .searchArchivedProcessInstances(
                        new ArchivedProcessInstanceApi.SearchArchivedProcessInstancesQueryParams()
                                .c(1)
                                .p(0)
                                .f(List.of("caller=any", "sourceObjectId=" + id)));
        if (!archivedInstances.isEmpty()) {
            return archivedInstances.get(0);
        }
        return null;
    }
}
