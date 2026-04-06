package com.bonitasoft.connectors.adobesign;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.*;

class CreateFromTemplateConnectorPropertyTest {

    @Property
    void mandatoryLibraryDocumentIdRejectsBlank(@ForAll("blankStrings") String input) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key")
                .libraryDocumentId(input).agreementName("Test").signerEmail("a@b.com").build();
        var connector = new CreateFromTemplateConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("libraryDocumentId");
    }

    @Property
    void mandatoryAgreementNameRejectsBlank(@ForAll("blankStrings") String input) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key")
                .libraryDocumentId("lib-123").agreementName(input).signerEmail("a@b.com").build();
        var connector = new CreateFromTemplateConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agreementName");
    }

    @Property
    void mandatorySignerEmailRejectsBlank(@ForAll("blankStrings") String input) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key")
                .libraryDocumentId("lib-123").agreementName("Test").signerEmail(input).build();
        var connector = new CreateFromTemplateConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signerEmail");
    }

    @Property
    void validConfigurationBuildsSuccessfully(
            @ForAll @StringLength(min = 1, max = 200) String libId,
            @ForAll @StringLength(min = 1, max = 200) String name,
            @ForAll @StringLength(min = 3, max = 100) String email
    ) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key")
                .libraryDocumentId(libId).agreementName(name).signerEmail(email).build();
        assertThat(config.getLibraryDocumentId()).isEqualTo(libId);
    }

    @Property
    void integrationKeyRejectsBlankInIntegrationKeyMode(@ForAll("blankStrings") String input) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey(input)
                .libraryDocumentId("lib-123").agreementName("Test").signerEmail("a@b.com").build();
        var connector = new CreateFromTemplateConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("integrationKey");
    }

    @Property
    void oauth2RequiresClientId(@ForAll("blankStrings") String input) {
        var config = AdobeSignConfiguration.builder()
                .authMode("OAUTH2").clientId(input).clientSecret("secret").refreshToken("token")
                .libraryDocumentId("lib-123").agreementName("Test").signerEmail("a@b.com").build();
        var connector = new CreateFromTemplateConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientId");
    }

    @Property
    void oauth2RequiresClientSecret(@ForAll("blankStrings") String input) {
        var config = AdobeSignConfiguration.builder()
                .authMode("OAUTH2").clientId("id").clientSecret(input).refreshToken("token")
                .libraryDocumentId("lib-123").agreementName("Test").signerEmail("a@b.com").build();
        var connector = new CreateFromTemplateConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientSecret");
    }

    @Property
    void oauth2RequiresRefreshToken(@ForAll("blankStrings") String input) {
        var config = AdobeSignConfiguration.builder()
                .authMode("OAUTH2").clientId("id").clientSecret("secret").refreshToken(input)
                .libraryDocumentId("lib-123").agreementName("Test").signerEmail("a@b.com").build();
        var connector = new CreateFromTemplateConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refreshToken");
    }

    @Property
    void invalidAuthModeIsRejected(@ForAll @StringLength(min = 1, max = 50) String authMode) {
        Assume.that(!authMode.equals("INTEGRATION_KEY") && !authMode.equals("OAUTH2"));
        var config = AdobeSignConfiguration.builder()
                .authMode(authMode)
                .libraryDocumentId("lib-123").agreementName("Test").signerEmail("a@b.com").build();
        var connector = new CreateFromTemplateConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    void defaultLocaleIsEnUs() {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key").build();
        assertThat(config.getLocale()).isEqualTo("en_US");
    }

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of("", " ", "\t", "\n");
    }
}
