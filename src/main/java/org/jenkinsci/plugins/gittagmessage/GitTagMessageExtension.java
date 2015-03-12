package org.jenkinsci.plugins.gittagmessage;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.GitSCMExtensionDescriptor;
import hudson.plugins.git.util.BuildData;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.logging.Logger;

import static hudson.Util.fixEmpty;
import static hudson.Util.fixEmptyAndTrim;

public class GitTagMessageExtension extends GitSCMExtension {

    private static final Logger LOGGER = Logger.getLogger(GitTagMessageExtension.class.getName());

    @DataBoundConstructor
    public GitTagMessageExtension() {
        // No configuration options
    }

    @Override
    public void onCheckoutCompleted(GitSCM scm, AbstractBuild<?, ?> build, GitClient git, BuildListener listener)
            throws IOException, InterruptedException, GitException {
        // Now that checkout is complete, grab the commit info that we'll be working with
        BuildData buildData = build.getAction(BuildData.class);
        if (buildData == null || buildData.getLastBuiltRevision() == null) {
            LOGGER.info("Git build information is not set; will not search for git tag message.");
            return;
        }

        Revision revision = buildData.getLastBuiltRevision();
        String commit = revision.getSha1String();
        String branch = revision.getBranches().iterator().next().getName();

        // If the refspec used explicitly searches for tags, then we should use the tag name that triggered this build.
        // If we don't do this, i.e. we just run "git describe" on the commit hash, it may return a different, newer tag
        String tagName;
        if (branch.contains("/tags/")) {
            int index = branch.indexOf("/tags/");
            tagName = branch.substring(index + "/tags/".length());
        } else {
            // This build was triggered for a named branch, or for a particular commit hash
            tagName = getTagName(git, commit);
            if (tagName == null) {
                return;
            }
        }

        // Retrieve the tag message for the given tag name, then store it
        try {
            String tagMessage = git.getTagMessage(tagName); // "git tag -l <tag> -n10000"
            build.addAction(new GitTagMessageAction(tagMessage));
            LOGGER.finest(String.format("Exporting tag message '%s'.", tagMessage));
        } catch (StringIndexOutOfBoundsException e) {
            // git-client currently throws this exception if you ask for the message of a non-existent tag
            LOGGER.info(String.format("No tag message exists for '%s'.", tagName));
        }
    }

    /** @return Tag name associated with the given commit, or {@code null} if there is none. */
    private static String getTagName(GitClient git, String commit) throws InterruptedException {
        // Query information about the most recent tag reachable from this commit
        String tagDescription = null;
        try {
            // This should return a tag name (e.g. "beta42") or the nearest tag name and an offset ("beta42-5-g123abcd")
            tagDescription = fixEmpty(git.describe(commit)); // "git describe --tags <commit>"
        } catch (GitException e) {
            // If there are no tags nearby, git returns a non-zero exit code, which throws this exception
            LOGGER.warning(String.format("Fetching tag info for '%s' threw exception: %s", commit, e.getMessage()));
        }
        if (tagDescription == null) {
            LOGGER.fine(String.format("No tag info could be found for '%s'; will not fetch tag message.", commit));
            return null;
        }

        // If "git describe" returns a value with offset, then this particular commit has no tag pointing to it
        if (tagDescription.matches(".+-[0-9]+-g[0-9A-Fa-f]{7}$")) {
            LOGGER.fine(String.format("Commit '%s' has no tag associated; will not fetch tag message.", commit));
            return null;
        }

        return fixEmptyAndTrim(tagDescription);
    }

    @Extension
    public static class DescriptorImpl extends GitSCMExtensionDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }
    }

}