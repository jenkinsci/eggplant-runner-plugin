
package io.jenkins.plugins.eggplant;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

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

public class EggplantRunnerBuilderTest {
    
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    
    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        builder.setServerURL("http://localhost:8080");
        builder.setTestConfigId("test-Config-Id");
        builder.setClientId("client-Id");
        builder.setClientSecret(hudson.util.Secret.fromString("c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        builder.setDryRun(true);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Dry run of test configuration test-Config-Id against server http://localhost:8080"), build);
    }

    @Test
    public void testBuildWithTestConfigId() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        builder.setServerURL("http://localhost:8080");
        builder.setTestConfig(new TestConfigId("test-Config-Id"));
        builder.setClientId("client-Id");
        builder.setClientSecret(hudson.util.Secret.fromString("c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        builder.setDryRun(true);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Dry run of test configuration test-Config-Id against server http://localhost:8080"), build);
    }

    @Test
    public void testBuildWithModelBasedTestConfigName() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        builder.setServerURL("http://localhost:8080");
        builder.setTestConfig(new ModelBased("test-Config-Name","model-Name"));
        builder.setClientId("client-Id");
        builder.setClientSecret(hudson.util.Secret.fromString("c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        builder.setDryRun(true);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Dry run of test configuration test-Config-Name against server http://localhost:8080"), build);
    }

    @Test
    public void testBuildWithScriptBasedTestConfigName() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        builder.setServerURL("http://localhost:8080");
        builder.setTestConfig(new ScriptBased("test-Config-Name","suite-Name"));
        builder.setClientId("client-Id");
        builder.setClientSecret(hudson.util.Secret.fromString("c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        builder.setDryRun(true);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Dry run of test configuration test-Config-Name against server http://localhost:8080"), build);
    }

    @Test
    public void testIsValidTestResultPath() throws Exception {
        DescriptorImpl descriptorImpl=new DescriptorImpl();

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
    public void testGetBackwardCompatibilityCommands() throws Exception {

        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        BuilderException exception = assertThrows(BuilderException.class, ()-> builder.getBackwardCompatibilityCommands());
        String expectedMessage = "testConfigId and testConfigName not found. Use only testConfigId or testConfigName (with modelName or suiteName) to continue.";
        String actualMessage = exception.getMessage();    
        assertTrue(actualMessage.contains(expectedMessage)); 
        
        builder.setTestConfigName("test-Config-Name");
        exception = assertThrows(BuilderException.class, ()-> builder.getBackwardCompatibilityCommands());
        expectedMessage = "testConfigName found, suiteName or modelName is required.";
        actualMessage = exception.getMessage();    
        assertTrue(actualMessage.contains(expectedMessage)); 

        builder.setModelName("model-Name");
        builder.setSuiteName("suite-Name");
        exception = assertThrows(BuilderException.class, ()-> builder.getBackwardCompatibilityCommands());
        expectedMessage = "modelName and suiteName found,  Use testConfigName with only suiteName or modelName to continue.";
        actualMessage = exception.getMessage();    
        assertTrue(actualMessage.contains(expectedMessage)); 

    }

    @Test
    public void testGetManadatoryCommandList(){

        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        List<String> command = new ArrayList<String>();

        builder.setServerURL("http://localhost:8080");
        builder.setClientId("dai-client-Id");
        builder.setClientSecret(hudson.util.Secret.fromString("c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        builder.setTestConfig(new TestConfigId("test-Config-Id"));
        command =  builder.getMandatoryCommandList(new EnvVars());
        assertTrue(command.contains("http://localhost:8080")); 
        assertTrue(command.contains("--client-id=dai-client-Id"));
        assertTrue(command.contains("--client-secret=c38ce33d-5644-4198-b28f-9cf3d9ac05e4")); 
        assertTrue(command.contains("test-Config-Id")); 

        builder.setTestConfig(new ModelBased("test-Config-Name","model-Name"));
        command =  builder.getMandatoryCommandList(new EnvVars());
        assertTrue(command.contains("modelbased")); 
        assertTrue(command.contains("--test-config-name=test-Config-Name")); 
        assertTrue(command.contains("--model-name=model-Name"));

        builder.setTestConfig(new ScriptBased("test-Config-Name","suite-Name"));
        command =  builder.getMandatoryCommandList(new EnvVars());
        assertTrue(command.contains("scriptbased")); 
        assertTrue(command.contains("--test-config-name=test-Config-Name")); 
        assertTrue(command.contains("--suite-name=suite-Name"));
        
    }

    @Test
    public void testOptionalCommandList(){
        
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        List<String> command = new ArrayList<String>();

        builder.setLogLevel(LogLevel.WARNING);
        command =  builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--log-level=WARNING"));

        builder.setCACertPath("cert path");
        command =  builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--ca-cert-path=cert path"));

        builder.setTestResultPath("result path");
        command =  builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--test-result-path=result path"));

        builder.setRequestTimeout("5");
        command =  builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--request-timeout=5"));

        builder.setRequestRetries("5");
        command =  builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--request-retries=5"));

        builder.setBackoffFactor("5");
        command =  builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--backoff-factor=5"));

        builder.setDryRun(true);
        command =  builder.getOptionalCommandList(OperatingSystem.WINDOWS);
        assertTrue(command.contains("--dry-run"));

    }

}