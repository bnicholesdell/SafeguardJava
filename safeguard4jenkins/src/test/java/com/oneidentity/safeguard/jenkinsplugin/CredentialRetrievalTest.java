package com.oneidentity.safeguard.jenkinsplugin;

import com.oneidentity.safeguard.jenkinsplugin.CredentialRetrieval;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CredentialRetrievalTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String safeguardAppliance = "1.1.1.1";
    final String certificatePath = "c:\\something.p12";
    final String passPhrase = "somesecret";
    final String apiKey = "myapikey";

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new CredentialRetrieval(safeguardAppliance, certificatePath, passPhrase, apiKey));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new CredentialRetrieval(safeguardAppliance, certificatePath, passPhrase, apiKey), project.getBuildersList().get(0));
    }

//    @Test
//    public void testConfigRoundtripFrench() throws Exception {
//        FreeStyleProject project = jenkins.createFreeStyleProject();
//        CredentialRetrieval builder = new CredentialRetrieval(name);
//        builder.setUseFrench(true);
//        project.getBuildersList().add(builder);
//        project = jenkins.configRoundtrip(project);
//
//        CredentialRetrieval lhs = new CredentialRetrieval(name);
//        lhs.setUseFrench(true);
//        jenkins.assertEqualDataBoundBeans(lhs, project.getBuildersList().get(0));
//    }

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        CredentialRetrieval builder = new CredentialRetrieval(safeguardAppliance, certificatePath, passPhrase, apiKey);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Certificate Path, " + certificatePath, build);
    }

//    @Test
//    public void testBuildFrench() throws Exception {
//
//        FreeStyleProject project = jenkins.createFreeStyleProject();
//        CredentialRetrieval builder = new CredentialRetrieval(name);
//        builder.setUseFrench(true);
//        project.getBuildersList().add(builder);
//
//        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
//        jenkins.assertLogContains("Bonjour, " + name, build);
//    }

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  greet '" + certificatePath + "'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        String expectedString = "Certificate Path, " + certificatePath + "!";
        jenkins.assertLogContains(expectedString, completedBuild);
    }

}