package org.jenkinsci.plugins.gittagmessage;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;

/** Exports the message text associated with a git tag used for a build. */
public class GitTagMessageAction implements EnvironmentContributingAction {

    /** The name of the environment variable this plugin exports. */
    static final String ENV_VAR_NAME_MESSAGE = "GIT_TAG_MESSAGE";
    static final String ENV_VAR_NAME_TAG = "GIT_TAG_NAME";

    private final String tagMessage;
    private final String tagName;

    public GitTagMessageAction(String tagName, String tagMessage) {
        this.tagMessage = tagMessage;
        this.tagName = tagName;
    }

    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        if (tagMessage != null) {
            env.put(ENV_VAR_NAME_MESSAGE, tagMessage);
        }
        if (tagName != null){
            env.put(ENV_VAR_NAME_TAG, tagName);
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