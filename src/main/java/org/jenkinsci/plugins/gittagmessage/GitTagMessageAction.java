package org.jenkinsci.plugins.gittagmessage;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;

/** Exports the message text associated with a git tag used for a build. */
public class GitTagMessageAction implements EnvironmentContributingAction {

    private final String tagMessage;

    public GitTagMessageAction(String tagMessage) {
        this.tagMessage = tagMessage;
    }

    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        if (tagMessage != null) {
            env.put("GIT_TAG_MESSAGE", tagMessage);
        }
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

}