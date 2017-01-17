package org.jenkinsci.plugins.gittagmessage;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.util.BuildData;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Collections;

import static org.jenkinsci.plugins.gittagmessage.GitTagMessageAction.ENV_VAR_NAME_MESSAGE;
import static org.jenkinsci.plugins.gittagmessage.GitTagMessageAction.ENV_VAR_NAME_TAG;
import static org.junit.Assert.assertNotNull;

public class GitTagMessageExtensionTest {

    @Rule public final JenkinsRule jenkins = new JenkinsRule();

    @Rule public final TemporaryFolder repoDir = new TemporaryFolder();

    private GitClient repo;

    @Before
    public void setUp() throws IOException, InterruptedException {
        // Set up a temporary git repository for each test case
        repo = Git.with(jenkins.createTaskListener(), null).in(repoDir.getRoot()).getClient();
        repo.init();
    }

    @Test
    public void commitWithoutTagShouldNotExportMessage() throws Exception {
        // Given a git repo without any tags
        repo.commit("commit 1");

        // When a build is executed
        FreeStyleProject job = configureGitTagMessageJob();
        FreeStyleBuild build = buildJobAndAssertSuccess(job);

        // Then no git tag information should have been exported
        assertBuildEnvironment(build, null, null);
    }

    @Test
    public void commitWithEmptyTagMessageShouldNotExportMessage() throws Exception {
        // Given a git repo which has been tagged, but without a message
        repo.commit("commit 1");
        repo.tag("release-1.0", null);

        // When a build is executed
        FreeStyleProject job = configureGitTagMessageJob();
        FreeStyleBuild build = buildJobAndAssertSuccess(job);

        // Then the git tag name message, but no message should have been exported
        assertBuildEnvironment(build, "release-1.0", null);
    }

    @Test
    public void commitWithTagShouldExportMessage() throws Exception {
        // Given a git repo which has been tagged
        repo.commit("commit 1");
        repo.tag("release-1.0", "This is the first release. ");

        // When a build is executed
        FreeStyleProject job = configureGitTagMessageJob();
        FreeStyleBuild build = buildJobAndAssertSuccess(job);

        // Then the (trimmed) git tag message should have been exported
        assertBuildEnvironment(build, "release-1.0", "This is the first release.");
    }

    @Test
    public void commitWithMultipleTagsShouldExportMessage() throws Exception {
        // Given a commit with multiple tags pointing to it
        repo.commit("commit 1");
        repo.tag("release-candidate-1.0", "This is the first release candidate.");
        repo.tag("release-1.0", "This is the first release.");
        // TODO: JGit seems to list tags in alphabetical order rather than in reverse chronological order

        // When a build is executed
        FreeStyleProject job = configureGitTagMessageJob();
        FreeStyleBuild build = buildJobAndAssertSuccess(job);

        // Then the most recent tag info should have been exported
        assertBuildEnvironment(build, "release-1.0", "This is the first release.");
    }

    @Test
    public void jobWithMatchingTagShouldExportThatTagMessage() throws Exception {
        // Given a commit with multiple tags pointing to it
        repo.commit("commit 1");
        repo.tag("alpha/1", "Alpha #1");
        repo.tag("beta/1", "Beta #1");
        repo.tag("gamma/1", "Gamma #1");

        // When a build is executed which is configured to only build beta/* tags
        FreeStyleProject job = configureGitTagMessageJob("+refs/tags/beta/*:refs/remotes/origin/tags/beta/*",
                "*/tags/beta/*");
        FreeStyleBuild build = buildJobAndAssertSuccess(job);

        // Then the selected tag info should be exported, even although it's not the latest tag
        assertBuildEnvironment(build, "beta/1", "Beta #1");
    }

    /** Asserts that the given build exported tag information, or not, if {@code null}. */
    private void assertBuildEnvironment(FreeStyleBuild build, String expectedName, String expectedMessage)
            throws Exception {
        jenkins.assertLogContains(String.format("tag='%s'", expectedName == null ? "" : expectedName), build);
        jenkins.assertLogContains(String.format("msg='%s'", expectedMessage == null ? "" : expectedMessage), build);
    }

    /**
     * Builds the given job and asserts that it succeeded, and the Git SCM ran.
     *
     * @param job The job to build.
     * @return The build that was executed.
     */
    private FreeStyleBuild buildJobAndAssertSuccess(FreeStyleProject job) throws Exception {
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(job);
        assertNotNull(build.getAction(BuildData.class));
        return build;
    }

    /** @return A job configured with the test Git repo, default settings, and the Git Tag Message extension. */
    private FreeStyleProject configureGitTagMessageJob() throws Exception {
        return configureGitTagMessageJob("", "**");
    }

    /**
     * @param refSpec The refspec to check out.
     * @param branchSpec The branch spec to build.
     * @return A job configured with the test Git repo, given settings, and the Git Tag Message extension.
     */
    private FreeStyleProject configureGitTagMessageJob(String refSpec, String branchSpec) throws Exception {
        UserRemoteConfig remote = new UserRemoteConfig(repoDir.getRoot().getAbsolutePath(), "origin", refSpec, null);
        GitSCM scm = new GitSCM(
                Collections.singletonList(remote),
                Collections.singletonList(new BranchSpec(branchSpec)),
                false, Collections.<SubmoduleConfig>emptyList(),
                null, null,
                Collections.<GitSCMExtension>singletonList(new GitTagMessageExtension()));

        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.getBuildersList().add(new Shell("echo \"tag='${" + ENV_VAR_NAME_TAG + "}'\""));
        job.getBuildersList().add(new Shell("echo \"msg='${" + ENV_VAR_NAME_MESSAGE + "}'\""));
        job.setScm(scm);
        return job;
    }

}