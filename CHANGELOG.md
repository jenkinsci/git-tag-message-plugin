# Version history

## 1.6.1
December 24, 2017

- Removed an unnecessary dependency on a Pipeline plugin, added in version 1.6
- Fixed integration tests so that they can run on Windows

## 1.6
December 20, 2017

- Added the ability to use the most recent tag to the commit being built ([JENKINS-32208](https://issues.jenkins-ci.org/browse/JENKINS-32208))
  - Thanks to [Arnaud Tamaillon](https://github.com/Greybird)
- Fixed the plugin to work with repos where git returns commit hash prefixes longer than seven characters

## 1.5
April 25, 2016

- Fixed crash which could happen with a detached HEAD, i.e. no branch name was associated with a commit ([JENKINS-34429](https://issues.jenkins-ci.org/browse/JENKINS-34429))

## 1.4
June 21, 2015

- The git tag name is now also exported as `GIT_TAG_NAME` ([JENKINS-28705](https://issues.jenkins-ci.org/browse/JENKINS-28705))
  - Thanks to Thomas Blitz

## 1.3
March 12, 2015

- Fixed crash when no tag name could be determined ([JENKINS-27383](https://issues.jenkins-ci.org/browse/JENKINS-27383))
- The outcome of this plugin (i.e. whether a tag message was found and exported) is now written to the build log
- Increased Jenkins requirement to 1.565.1

## 1.2
February 1, 2015

- Ensured that, if building from a tag, the message from that tag is used (rather than a newer tag pointing at the same commit)

## 1.1
October 31, 2014

- Fixed crash when a detached HEAD or repo with no tags was used

## 1.0
October 22, 2014

- Initial release