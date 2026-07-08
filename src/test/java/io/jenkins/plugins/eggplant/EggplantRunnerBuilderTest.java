package io.jenkins.plugins.eggplant;

import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;
import io.jenkins.plugins.eggplant.EggplantRunnerBuilder.DescriptorImpl;
import io.jenkins.plugins.eggplant.EggplantRunnerBuilder.ModelBased;
import io.jenkins.plugins.eggplant.EggplantRunnerBuilder.ScriptBased;
import io.jenkins.plugins.eggplant.EggplantRunnerBuilder.TestConfigId;
import io.jenkins.plugins.eggplant.common.LogLevel;
import io.jenkins.plugins.eggplant.common.OperatingSystem;
import io.jenkins.plugins.eggplant.exception.BuilderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
class EggplantRunnerBuilderTest {

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        builder.setServerURL("http://localhost:8080");
        builder.setTestConfigId("test-Config-Id");
        builder.setClientId("client-Id");
        builder.setClientSecret(hudson.util.Secret.fromString("c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        builder.setDryRun(true);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Dry run of test configuration test-Config-Id against server http://localhost:8080", build);
    }

    @Test
    void testBuildWithTestConfigId() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        builder.setServerURL("http://localhost:8080");
        builder.setTestConfig(new TestConfigId("test-Config-Id"));
        builder.setClientId("client-Id");
        builder.setClientSecret(hudson.util.Secret.fromString("c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        builder.setDryRun(true);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Dry run of test configuration test-Config-Id against server http://localhost:8080", build);
    }

    @Test
    void testBuildWithModelBasedTestConfigName() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        builder.setServerURL("http://localhost:8080");
        builder.setTestConfig(new ModelBased("test-Config-Name", "space-Name", "model-Name"));
        builder.setClientId("client-Id");
        builder.setClientSecret(hudson.util.Secret.fromString("c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        builder.setDryRun(true);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Dry run of test configuration test-Config-Name against server http://localhost:8080", build);
    }

    @Test
    void testBuildWithScriptBasedTestConfigName() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        builder.setServerURL("http://localhost:8080");
        builder.setTestConfig(new ScriptBased("test-Config-Name", "space-Name", "suite-Name"));
        builder.setClientId("client-Id");
        builder.setClientSecret(hudson.util.Secret.fromString("c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        builder.setDryRun(true);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Dry run of test configuration test-Config-Name against server http://localhost:8080", build);
    }

    @Test
    void testIsValidTestResultPath() throws Exception {
        DescriptorImpl descriptorImpl = new DescriptorImpl();

        FormValidation form = descriptorImpl.doCheckTestResultPath("");
        assertEquals(FormValidation.Kind.OK, form.kind);

        form = descriptorImpl.doCheckTestResultPath("C:\\TestResultPath\\testResultFile.xml");
        assertEquals(FormValidation.Kind.OK, form.kind);

        form = descriptorImpl.doCheckTestResultPath("\\TestResultPath\\testResultFile.xml");
        assertEquals(FormValidation.Kind.OK, form.kind);

        form = descriptorImpl.doCheckTestResultPath("testResultFile.xml");
        assertEquals(FormValidation.Kind.OK, form.kind);

        form = descriptorImpl.doCheckTestResultPath("testResultFile\\.xml");
        assertEquals(FormValidation.Kind.OK, form.kind);

        form = descriptorImpl.doCheckTestResultPath("testResultFile/.xml");
        assertEquals(FormValidation.Kind.OK, form.kind);

        form = descriptorImpl.doCheckTestResultPath("testResultFile:.xml");
        assertEquals(FormValidation.Kind.OK, form.kind);

        form = descriptorImpl.doCheckTestResultPath("                ");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

        form = descriptorImpl.doCheckTestResultPath("C:\\TestResultPath");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

        form = descriptorImpl.doCheckTestResultPath("C:\\TestResultPath\\testResultFile.txt");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

        form = descriptorImpl.doCheckTestResultPath("testResultFile\".xml");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

        form = descriptorImpl.doCheckTestResultPath("testResultFile?.xml");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

        form = descriptorImpl.doCheckTestResultPath("testResultFile*.xml");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

        form = descriptorImpl.doCheckTestResultPath("testResultFile<.xml");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

        form = descriptorImpl.doCheckTestResultPath("testResultFile>.xml");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

        form = descriptorImpl.doCheckTestResultPath("testResultFile|.xml");
        assertEquals(FormValidation.Kind.ERROR, form.kind);
    }

    @Test
    void testGetBackwardCompatibilityCommands() {
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        BuilderException exception = assertThrows(BuilderException.class, builder::getBackwardCompatibilityCommands);
        String expectedMessage = "testConfigId and testConfigName not found. Use only testConfigId or testConfigName (with modelName or suiteName) to continue.";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));

        builder.setTestConfigName("test-Config-Name");
        exception = assertThrows(BuilderException.class, builder::getBackwardCompatibilityCommands);
        expectedMessage = "testConfigName found, suiteName or modelName is required.";
        actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));

        builder.setModelName("model-Name");
        builder.setSuiteName("suite-Name");
        exception = assertThrows(BuilderException.class, builder::getBackwardCompatibilityCommands);
        expectedMessage = "modelName and suiteName found,  Use testConfigName with only suiteName or modelName to continue.";
        actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testDefaultSpaceNameWhenNotProvided() throws BuilderException {
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        builder.setTestConfigName("test-Config-Name");
        builder.setModelName("model-Name");
        builder.getBackwardCompatibilityCommands();
        
        ModelBased testConfig = (ModelBased) builder.getTestConfig();
        assertEquals("Shared space", testConfig.getSpaceName());

        // Test with empty spaceName
        builder = new EggplantRunnerBuilder();
        builder.setTestConfigName("test-Config-Name");
        builder.setSpaceName("");
        builder.setSuiteName("suite-Name");
        builder.getBackwardCompatibilityCommands();
        
        ScriptBased scriptConfig = (ScriptBased) builder.getTestConfig();
        assertEquals("Shared space", scriptConfig.getSpaceName());
    }

    @Test
    void testSpaceNameHandlesNullAndEmpty() throws BuilderException {
        // Test that Java null defaults to "Shared space"
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        builder.setTestConfigName("test-Config-Name");
        builder.setSpaceName(null);
        builder.setModelName("model-Name");
        builder.getBackwardCompatibilityCommands();
        
        ModelBased testConfig = (ModelBased) builder.getTestConfig();
        assertEquals("Shared space", testConfig.getSpaceName());

        // Test with ScriptBased and empty string
        builder = new EggplantRunnerBuilder();
        builder.setTestConfigName("test-Config-Name");
        builder.setSpaceName("");
        builder.setSuiteName("suite-Name");
        builder.getBackwardCompatibilityCommands();
        
        ScriptBased scriptConfig = (ScriptBased) builder.getTestConfig();
        assertEquals("Shared space", scriptConfig.getSpaceName());
        
        // Test that the string "null" is preserved as a legitimate space name
        builder = new EggplantRunnerBuilder();
        builder.setTestConfigName("test-Config-Name");
        builder.setSpaceName("null");
        builder.setModelName("model-Name");
        builder.getBackwardCompatibilityCommands();
        
        testConfig = (ModelBased) builder.getTestConfig();
        assertEquals("null", testConfig.getSpaceName());
    }

    @Test
    void testSpaceNameInConstructor() {
        // Test that ModelBased and ScriptBased constructors handle null and empty correctly
        ModelBased modelBased = new ModelBased("test-Config-Name", null, "model-Name");
        assertEquals("Shared space", modelBased.getSpaceName());

        ScriptBased scriptBased = new ScriptBased("test-Config-Name", "", "suite-Name");
        assertEquals("Shared space", scriptBased.getSpaceName());

        // Test that the string "null" is preserved as a valid space name
        modelBased = new ModelBased("test-Config-Name", "null", "model-Name");
        assertEquals("null", modelBased.getSpaceName());

        scriptBased = new ScriptBased("test-Config-Name", "null", "suite-Name");
        assertEquals("null", scriptBased.getSpaceName());

        // Test that valid space names are preserved
        modelBased = new ModelBased("test-Config-Name", "Custom Space", "model-Name");
        assertEquals("Custom Space", modelBased.getSpaceName());

        scriptBased = new ScriptBased("test-Config-Name", "My Space", "suite-Name");
        assertEquals("My Space", scriptBased.getSpaceName());
    }

    @Test
    void testGetMandatoryCommandList() {
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        List<String> command;

        builder.setServerURL("http://localhost:8080");
        builder.setClientId("dai-client-Id");
        builder.setClientSecret(hudson.util.Secret.fromString("c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        builder.setTestConfig(new TestConfigId("test-Config-Id"));
        command = builder.getMandatoryCommandList(new EnvVars());
        assertTrue(command.contains("http://localhost:8080"));
        assertTrue(command.contains("--client-id=dai-client-Id"));
        assertTrue(command.contains("--client-secret=c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        assertTrue(command.contains("test-Config-Id"));

        builder.setTestConfig(new ModelBased("test-Config-Name", "space-Name", "model-Name"));
        command = builder.getMandatoryCommandList(new EnvVars());
        assertTrue(command.contains("modelbased"));
        assertTrue(command.contains("--test-config-name=test-Config-Name"));
        assertTrue(command.contains("--space-name=space-Name"));
        assertTrue(command.contains("--model-name=model-Name"));

        builder.setTestConfig(new ScriptBased("test-Config-Name", "space-Name", "suite-Name"));
        command = builder.getMandatoryCommandList(new EnvVars());
        assertTrue(command.contains("scriptbased"));
        assertTrue(command.contains("--test-config-name=test-Config-Name"));
        assertTrue(command.contains("--space-name=space-Name"));
        assertTrue(command.contains("--suite-name=suite-Name"));
        
        // Test that null spaceName gets converted to "Shared space" in CLI args (not the string "null")
        builder.setTestConfig(new ModelBased("test-Config-Name", null, "model-Name"));
        command = builder.getMandatoryCommandList(new EnvVars());
        assertTrue(command.contains("--space-name=Shared space"));
        assertFalse(command.contains("--space-name=null"));
        
        builder.setTestConfig(new ScriptBased("test-Config-Name", null, "suite-Name"));
        command = builder.getMandatoryCommandList(new EnvVars());
        assertTrue(command.contains("--space-name=Shared space"));
        assertFalse(command.contains("--space-name=null"));
        
        // Test that the string "null" is preserved as a legitimate space name
        builder.setTestConfig(new ModelBased("test-Config-Name", "null", "model-Name"));
        command = builder.getMandatoryCommandList(new EnvVars());
        assertTrue(command.contains("--space-name=null"));
    }

    @Test
    void testOptionalCommandList() {
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        List<String> command;

        builder.setLogLevel(LogLevel.WARNING);
        command = builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--log-level=WARNING"));

        builder.setCACertPath("cert path");
        command = builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--ca-cert-path=cert path"));

        builder.setTestResultPath("result path");
        command = builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--test-result-path=result path"));

        builder.setRequestTimeout("5");
        command = builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--request-timeout=5"));

        builder.setRequestRetries("5");
        command = builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--request-retries=5"));

        builder.setBackoffFactor("5");
        command = builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--backoff-factor=5"));

        builder.setDryRun(true);
        command = builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--dry-run"));
    }

}