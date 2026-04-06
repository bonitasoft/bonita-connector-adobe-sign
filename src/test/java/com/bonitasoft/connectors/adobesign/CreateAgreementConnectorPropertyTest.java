package com.bonitasoft.connectors.adobesign;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.*;

class CreateAgreementConnectorPropertyTest {

    @Property
    void mandatoryDocumentBase64RejectsBlank(@ForAll("blankStrings") String input) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key")
                .documentBase64(input).documentName("test.pdf")
                .agreementName("Agreement").signerEmail("a@b.com").build();
        var connector = new CreateAgreementConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documentBase64");
    }

    @Property
    void mandatoryDocumentNameRejectsBlank(@ForAll("blankStrings") String input) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key")
                .documentBase64("dGVzdA==").documentName(input)
                .agreementName("Agreement").signerEmail("a@b.com").build();
        var connector = new CreateAgreementConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documentName");
    }

    @Property
    void mandatoryAgreementNameRejectsBlank(@ForAll("blankStrings") String input) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key")
                .documentBase64("dGVzdA==").documentName("test.pdf")
                .agreementName(input).signerEmail("a@b.com").build();
        var connector = new CreateAgreementConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agreementName");
    }

    @Property
    void mandatorySignerEmailRejectsBlank(@ForAll("blankStrings") String input) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key")
                .documentBase64("dGVzdA==").documentName("test.pdf")
                .agreementName("Agreement").signerEmail(input).build();
        var connector = new CreateAgreementConnector();
        assertThatThrownBy(() -> connector.validateConfiguration(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signerEmail");
    }

    @Property
    void validConfigurationAlwaysBuilds(
            @ForAll @StringLength(min = 1, max = 200) String key,
            @ForAll @StringLength(min = 1, max = 200) String docName,
            @ForAll @StringLength(min = 1, max = 200) String agrName,
            @ForAll @StringLength(min = 3, max = 100) String email
    ) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey(key)
                .documentBase64("dGVzdA==").documentName(docName)
                .agreementName(agrName).signerEmail(email).build();
        assertThat(config).isNotNull();
        assertThat(config.getIntegrationKey()).isEqualTo(key);
    }

    @Property
    void regionAlwaysResolvesToValidBaseUrl(
            @ForAll("validRegions") String region
    ) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key")
                .region(region).build();
        assertThat(config.getEffectiveBaseUrl())
                .startsWith("https://api.")
                .contains("adobesign.com");
    }

    @Property
    void customBaseUrlOverridesRegion(
            @ForAll("customBaseUrls") String baseUrl
    ) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key")
                .region("na1").baseUrl(baseUrl).build();
        assertThat(config.getEffectiveBaseUrl()).doesNotContain("api.na1");
    }

    @Property
    void authorizationHeaderContainsBearerPrefix(
            @ForAll @StringLength(min = 1, max = 500) String key
    ) {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey(key).build();
        assertThat(config.getAuthorizationHeader()).startsWith("Bearer ");
        assertThat(config.getAuthorizationHeader()).contains(key);
    }

    @Property
    void defaultTimeoutsArePositive() {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key").build();
        assertThat(config.getConnectTimeout()).isGreaterThan(0);
        assertThat(config.getReadTimeout()).isGreaterThan(0);
    }

    @Property
    void signatureTypeDefaultsToEsign() {
        var config = AdobeSignConfiguration.builder()
                .authMode("INTEGRATION_KEY").integrationKey("key").build();
        assertThat(config.getSignatureType()).isEqualTo("ESIGN");
    }

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of("", " ", "\t", "\n");
    }

    @Provide
    Arbitrary<String> validRegions() {
        return Arbitraries.of("na1", "na2", "eu1", "eu2", "jp1", "au1");
    }

    @Provide
    Arbitrary<String> customBaseUrls() {
        return Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50)
                .map(s -> "https://custom-" + s + ".example.com");
    }
}
