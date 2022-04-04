package io.jenkins.plugins.eggplant;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

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
}
