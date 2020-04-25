# Git Tag Message plugin for Jenkins

[![Jenkins plugin](https://img.shields.io/jenkins/plugin/v/git-tag-message.svg)](https://plugins.jenkins.io/git-tag-message)
[![Jenkins plugin installs](https://img.shields.io/jenkins/plugin/i/git-tag-message?color=blue)](https://plugins.jenkins.io/git-tag-message)
[![Build status](https://ci.jenkins.io/job/Plugins/job/git-tag-message-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/git-tag-message-plugin/job/master/)

## Functionality

Exports the name and message for a Git tag as environment variables during a Freestyle build. If the revision checked out during a build has a Git tag associated with it, its name will be exported during the build as the `GIT_TAG_NAME` environment variable.

If a message was specified when creating the tag (e.g. via `git tag -m "..."`), then that message will also be exported during the build, as the `GIT_TAG_MESSAGE` environment variable.

If the revision has more than one tag associated with it, only the most recent tag will be taken into account. However, if your refspec includes `refs/tags` — i.e. builds are only triggered when certain tag names or patterns are matched — then the exact tag name that triggered the build will be used, even if it's not the most recent tag for this commit.

You can optionally choose the "use most recent tag" option, which will
then export the tag name and message from the nearest tag in the history
of the commit being built, if any.

## Usage

### Freestyle

Under the Source Control Management section in your job configuration, if you have selected "Git", then there should be a section labelled "Additional Behaviours".

Click "Add" and select "Export git tag and message as environment variables".

If you are also using the "Create a tag for every build" behaviour, use the drag-and-drop handles to ensure that it happens **after** the "Export git tag and message as environment variables" behaviour. Otherwise, the Git tag message will likely end up being the auto-generated Jenkins tag message, e.g. "Jenkins Build #1".

### Pipeline

Currently, this plugin does not work in Pipeline, as plugins can't contribute environment variables in the same way as in Freestyle.

However, until it's possible for this plugin to support Pipeline builds, you could use these Scripted Pipeline functions to achieve pretty much the same result:

```groovy
// Example usage
node {
    git url: 'https://github.com/jenkinsci/git-tag-message-plugin'
    env.GIT_TAG_NAME = gitTagName()
    env.GIT_TAG_MESSAGE = gitTagMessage()
}

/** @return The tag name, or `null` if the current commit isn't a tag. */
String gitTagName() {
    commit = getCommit()
    if (commit) {
        desc = sh(script: "git describe --tags ${commit}", returnStdout: true)?.trim()
        if (isTag(desc)) {
            return desc
        }
    }
    return null
}

/** @return The tag message, or `null` if the current commit isn't a tag. */
String gitTagMessage() {
    name = gitTagName()
    msg = sh(script: "git tag -n10000 -l ${name}", returnStdout: true)?.trim()
    if (msg) {
        return msg.substring(name.size()+1, msg.size())
    }
    return null
}

String getCommit() {
    return sh(script: 'git rev-parse HEAD', returnStdout: true)?.trim()
}

@NonCPS
boolean isTag(String desc) {
    match = desc =~ /.+-[0-9]+-g[0-9A-Fa-f]{6,}$/
    result = !match
    match = null // prevent serialisation
    return result
}
```

## Example

At [iosphere](https://iosphere.de/), we used this in the process of automatically building and distributing beta versions of our mobile apps:

1.  Commit some code
2.  Write or generate release notes, e.g. `./generateChangelog.sh > /tmp/changes`
3.  Create a tag, annotating it with the release notes, e.g. `git tag -F /tmp/changes beta/123`
4.  Push the commit and tag to the remote repo

Jenkins would then, having received a Git webhook notification:

1.  Trigger a build of the relevant job, e.g. 'trails-beta-distribution', which would:
2.  Check out the committed code, i.e. the revision at tag `beta/123`
3.  Export a `GIT_TAG_MESSAGE` variable — this will contain the release notes attached to the tag
4.  Build the mobile app
5.  Use the `${GIT_TAG_MESSAGE}` value to fill out the release notes text field in a post-build distribution step, e.g. [Google Play Android Publisher
    plugin](https://plugins.jenkins.io/google-play-android-publisher)
6.  Upload and deploy the app to the relevant service for distribution to users

## Changelog
See [CHANGELOG.md](https://github.com/jenkinsci/git-tag-message-plugin/blob/master/CHANGELOG.md).