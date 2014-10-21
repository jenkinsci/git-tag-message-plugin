package org.jenkinsci.plugins.gittagmessage;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.GitSCMExtensionDescriptor;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.logging.Logger;

import static hudson.Util.fixEmpty;

public class GitTagMessageExtension extends GitSCMExtension {

    private static final Logger LOGGER = Logger.getLogger(GitTagMessageExtension.class.getName());

    @DataBoundConstructor
    public GitTagMessageExtension() {
        // No configuration options
    }

    @Override
    public void onCheckoutCompleted(GitSCM scm, AbstractBuild<?, ?> build, GitClient git, BuildListener listener)
            throws IOException, InterruptedException, GitException {
        // Now that checkout is complete, grab the commit that we'll be working with
        final String commit = build.getEnvironment(listener).get(GitSCM.GIT_COMMIT);
        if (commit == null) {
            LOGGER.finest("GIT_COMMIT is not set; will not search for git tag message.");
            return;
        }

        // Query information about the most recent tag reachable from this commit
        final String tagDescription = fixEmpty(git.describe(commit));
        if (tagDescription == null) {
            LOGGER.finest(String.format("No tag info could be found for '%s'; will not fetch tag message.", commit));
            return;
        }

        // If we get a tag name, but with an offset, that means that the tag name returned belongs to another commit
        if (tagDescription.matches(".+-[0-9]+-g[0-9A-Fa-f]{7}$")) {
            LOGGER.finest(String.format("Commit '%s' has no tag associated; will not fetch tag message.", commit));
            return;
        }

        // Retrieve the tag message for the given tag name from git, then store it
        String tagMessage = null;
        try {
            tagMessage = git.getTagMessage(tagDescription);
        } catch (StringIndexOutOfBoundsException e) {
            // git-client currently throws this exception if you ask for the message of a non-existent tag
            LOGGER.info(String.format("No tag message exists for '%s'.", tagDescription));
        }
        build.addAction(new GitTagMessageAction(tagMessage));
        LOGGER.finest(String.format("Exporting tag message '%s'.", tagMessage));
    }

    @Extension
    public static class DescriptorImpl extends GitSCMExtensionDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }
    }

}