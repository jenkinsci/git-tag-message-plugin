# Git Tag Message plugin for Jenkins

During a Jenkins build that uses a git repository, this plugin checks whether
the revision checked out has a git tag associated with it, and whether a message
was specified when creating the tag.

If there is a git tag message available, the message will be made available
during the build via the `GIT_TAG_MESSAGE` environment variable.

## Example

This can be helpful, for example, when using Jenkins to generate a release build
with associated release notes.

e.g. At [iosphere][], we use a process like this to automate building and
distributing beta versions of our mobile apps:

1. Commit some code
2. Write or generate release notes, e.g. `./generateChangelog.sh > /tmp/changes`
3. Tag the branch, e.g. `git tag -a -F /tmp/changes beta/123`

Jenkins will then:

1. Check out the committed code
2. Set a `GIT_TAG_MESSAGE` variable, which will contain the app release notes
3. Use the `${GIT_TAG_MESSAGE}` value with the [HockeyApp][hockey] or [Google
Play Android Publisher][playpublisher] plugins.
4. The app is then deployed to the relevant service, complete with release notes

## More info
Jenkins wiki link will appear here, once this is an official plugin.

[iosphere]:https://iosphere.de/
[hockey]:https://wiki.jenkins-ci.org/display/JENKINS/HockeyApp+Plugin
[playpublisher]:https://wiki.jenkins-ci.org/display/JENKINS/Google+Play+Android+Publisher+Plugin