# Git Tag Message plugin for Jenkins

During a Jenkins build that uses a git repository, this plugin checks whether
the revision checked out has a git tag associated with it, and whether a message
was specified when creating the tag.

If there is a git tag message available, the message will be made available
during the build via the `GIT_TAG_MESSAGE` environment variable.

## Example

At [iosphere][], we use this in the process of automatically building and
distributing beta versions of our [mobile][trails] [apps][offmaps]:

1. Commit some code
2. Write or generate release notes, e.g. `./generateChangelog.sh > /tmp/changes`
3. Create a tag, annotating it with the release notes, e.g. `git tag -F
/tmp/changes beta/123`
4. Push the commit and tag to the remote repo

Jenkins will then, having received a git webhook notification:

1. Trigger a build of the relevant job, e.g. 'trails-beta-distribution'
2. Check out the committed code, i.e. the revision at tag `beta/123`
3. Export a `GIT_TAG_MESSAGE` variable — this will contain the release notes
4. Build the mobile app
5. Use the `${GIT_TAG_MESSAGE}` value to fill out the release notes textfield
in a post-build step, e.g. the [HockeyApp][hockey] or [Google Play Android
Publisher][playpublisher] plugin
6. Upload and deploy the app to the relevant service for distribution to users

## More info
https://wiki.jenkins-ci.org/display/JENKINS/Git+Tag+Message+Plugin

[iosphere]:https://iosphere.de/
[trails]:https://trails.io/ "Trails: Hiking GPS tracker for iPhone"
[offmaps]:http://offmaps.com/ "OffMaps: Offline maps for iOS"
[hockey]:https://wiki.jenkins-ci.org/display/JENKINS/HockeyApp+Plugin "Automate publishing mobile and desktop apps to HockeyApp with Jenkins"
[playpublisher]:https://wiki.jenkins-ci.org/display/JENKINS/Google+Play+Android+Publisher+Plugin "Automate publishing Android APKs with Jenkins"
