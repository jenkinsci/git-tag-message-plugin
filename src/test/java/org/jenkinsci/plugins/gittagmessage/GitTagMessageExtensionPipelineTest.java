package org.jenkinsci.plugins.gittagmessage;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Ignore;

import static org.jenkinsci.plugins.gittagmessage.GitTagMessageAction.ENV_VAR_NAME_MESSAGE;
import static org.jenkinsci.plugins.gittagmessage.GitTagMessageAction.ENV_VAR_NAME_TAG;

@Ignore("Disabled, as the extension does not work in Pipeline builds")
public class GitTagMessageExtensionPipelineTest extends AbstractGitTagMessageExtensionTest<WorkflowJob, WorkflowRun> {

    /**
     * @param refSpec The refspec to check out.
     * @param branchSpec The branch spec to build.
     * @param useMostRecentTag true to use the most recent tag rather than the exact one.
     * @return A job configured with the test Git repo, given settings, and the Git Tag Message extension.
     */
    protected WorkflowJob configureGitTagMessageJob(String refSpec, String branchSpec, boolean useMostRecentTag) throws Exception {
        String gitStep = String.format(
            "checkout([$class: 'GitSCM', "
                + "userRemoteConfigs: [[url: '%s', refspec: '%s']], "
                + "branches: [[name: '%s']], "
                + "extensions: [[$class: 'GitTagMessageExtension', useMostRecentTag: %b]]"
                + "])",
            repoDir.getRoot().getAbsolutePath(), refSpec, branchSpec, useMostRecentTag);

        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "j");
        job.setDefinition(new CpsFlowDefinition(""
                + "node() {"
                +    gitStep + "\n"
                + "  echo \"tag='${env." + ENV_VAR_NAME_TAG + " }'\"\n"
                + "  echo \"msg='${env." + ENV_VAR_NAME_MESSAGE + "}'\"\n"
                + "}", true));
        return job;
    }

    /** Asserts that the most recent build of the given job exported a tag message, or not exported if {@code null}. */
    protected void assertBuildEnvironment(WorkflowRun build, String expectedName, String expectedMessage)
            throws Exception {
        // In Pipeline, unknown environment variables are returned as null
        jenkins.assertLogContains(String.format("tag='%s'", expectedName), build);
        jenkins.assertLogContains(String.format("msg='%s'", expectedMessage), build);
    }

}