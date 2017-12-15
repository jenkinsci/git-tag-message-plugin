package org.jenkinsci.plugins.gittagmessage;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.GitSCMExtensionDescriptor;
import hudson.plugins.git.util.BuildData;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hudson.Util.fixEmpty;
import static hudson.Util.fixEmptyAndTrim;
import static org.jenkinsci.plugins.gittagmessage.GitTagMessageAction.ENV_VAR_NAME_MESSAGE;
import static org.jenkinsci.plugins.gittagmessage.GitTagMessageAction.ENV_VAR_NAME_TAG;

public class GitTagMessageExtension extends GitSCMExtension {

    private static final Logger LOGGER = Logger.getLogger(GitTagMessageExtension.class.getName());
    private static final Pattern TAG_OFFSET_MATCHER = Pattern.compile("(?<tag>.+)-[0-9]+-g[0-9A-Fa-f]{7}$");

    private boolean useMostRecentTag;

    @DataBoundConstructor
    public GitTagMessageExtension() {
        // No configuration options
    }

    @DataBoundSetter
    public void setUseMostRecentTag(boolean value) {
        useMostRecentTag = value;
    }

    public boolean isUseMostRecentTag() {
        return useMostRecentTag;
    }

    @Override
    public void onCheckoutCompleted(GitSCM scm, Run<?, ?> build, GitClient git, TaskListener listener)
            throws IOException, InterruptedException, GitException {
        // Now that checkout is complete, grab the commit info that we'll be working with
        BuildData buildData = build.getAction(BuildData.class);
        if (buildData == null || buildData.getLastBuiltRevision() == null) {
            LOGGER.info("Git build information is not set; will not search for git tag message.");
            return;
        }

        Revision revision = buildData.getLastBuiltRevision();
        String commit = null;
        Collection<Branch> branches = null;
        if (revision != null) {
            commit = revision.getSha1String();
            if (commit != null) {
                branches = revision.getBranches();
            }
        }

        // Try and get the branch name; this may be null if the repo is in a detached HEAD state.
        // If we can't get it, it's no problem; we'll look for a tag associated with the commit hash
        String branchName = null;
        if (branches != null && !branches.isEmpty()) {
            Branch branch = branches.iterator().next();
            if (branch != null) {
                branchName = branch.getName();
            }
        }

        // If the refspec used explicitly searches for tags, then we should use the tag name that triggered this build.
        // If we don't do this, i.e. we just run "git describe" on the commit hash, it may return a different, newer tag
        String tagName;
        if (branchName != null && branchName.contains("/tags/")) {
            int index = branchName.indexOf("/tags/");
            tagName = branchName.substring(index + "/tags/".length());
        } else {
            // This build was triggered for a named branch, or for a particular commit hash
            tagName = getTagName(git, commit, useMostRecentTag);
            if (tagName == null) {
                listener.getLogger().println(Messages.NoTagFound());
                return;
            }
        }

        // Attempt to retrieve the tag message for the discovered tag name
        try {
            String tagMessage = git.getTagMessage(tagName); // "git tag -l <tag> -n10000"
            // Empty or whitespace-only values aren't exported to the environment by Jenkins, so we can trim the message
            tagMessage = fixEmptyAndTrim(tagMessage);
            if (tagMessage == null) {
                listener.getLogger().println(Messages.NoTagMessageFound(tagName));
                LOGGER.finest(String.format("No tag message could be determined for git tag '%s'.", tagName));
            } else {
                listener.getLogger().println(Messages.ExportingTagMessage(ENV_VAR_NAME_MESSAGE, tagName));
                LOGGER.finest(String.format("Exporting tag message '%s' from tag '%s'.", tagMessage, tagName));
            }

            // Always export the tag name itself
            listener.getLogger().println(Messages.ExportingTagName(ENV_VAR_NAME_TAG, tagName));
            LOGGER.finest(String.format("Exporting git tag name '%s'", tagName));

            // Add the action which will export the variables
            build.addAction(new GitTagMessageAction(tagName, tagMessage));
        } catch (StringIndexOutOfBoundsException e) {
            // git-client currently throws this exception if you ask for the message of a non-existent tag
            LOGGER.info(String.format("No tag message exists for '%s'.", tagName));
        }
    }

    /** @return Tag name associated with the given commit, or {@code null} if there is none. */
    private static String getTagName(GitClient git, String commit, boolean allowOffsetedTags) throws InterruptedException {
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
        Matcher m = TAG_OFFSET_MATCHER.matcher(tagDescription);
        if (m.matches()) {
            if (allowOffsetedTags) {
                tagDescription = m.group("tag");
            } else {
                LOGGER.fine(String.format("Commit '%s' has no tag associated; will not fetch tag message.", commit));
                return null;
            }
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