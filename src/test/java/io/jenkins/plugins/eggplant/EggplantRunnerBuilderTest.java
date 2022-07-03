
package io.jenkins.plugins.eggplant;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;
import io.jenkins.plugins.eggplant.EggplantRunnerBuilder.DescriptorImpl;

public class EggplantRunnerBuilderTest {
    
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    
    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        EggplantRunnerBuilder builder = new EggplantRunnerBuilder();
        builder.setServerURL("http://localhost:8080");
        builder.setTestConfigId("test-Config-Id");
        builder.setClientSecret(hudson.util.Secret.fromString("c38ce33d-5644-4198-b28f-9cf3d9ac05e4"));
        builder.setDryRun(true);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(String.format("Dry run of test configuration test-Config-Id against server http://localhost:8080"), build);
    }

    @Test
    public void testIsValidTestResultPath() throws Exception {
        DescriptorImpl descriptorImpl=new DescriptorImpl();

        FormValidation form = descriptorImpl.doCheckTestResultPath("");
        assertEquals(FormValidation.Kind.OK, form.kind);        

        form = descriptorImpl.doCheckTestResultPath("                ");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

        form = descriptorImpl.doCheckTestResultPath("C:\\TestResultPath");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

        form = descriptorImpl.doCheckTestResultPath("C:\\TestResultPath\\testResultFile.txt");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

        form = descriptorImpl.doCheckTestResultPath("testResultFile|.xml");
        assertEquals(FormValidation.Kind.ERROR, form.kind);

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
     }
   
}