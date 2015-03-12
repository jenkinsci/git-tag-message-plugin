package org.jenkinsci.plugins.gittagmessage;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;

/** Exports the message text associated with a git tag used for a build. */
public class GitTagMessageAction implements EnvironmentContributingAction {

    /** The name of the environment variable this plugin exports. */
    static final String ENV_VAR_NAME = "GIT_TAG_MESSAGE";

    private final String tagMessage;

    public GitTagMessageAction(String tagMessage) {
        this.tagMessage = tagMessage;
    }

    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        if (tagMessage != null) {
            env.put(ENV_VAR_NAME, tagMessage);
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