# Jenkins Pull-Request Integration

This started as a project "how difficult could it be" to create a integration between Stash and Jenkins CI. 
So I decided to build a small plugin for Stash inspired by all the examples provided by Atlassian

This stash plugin hook into create/update/reopen pull-request to trigger a build on Jenkins CI through the API. 
3 hooks listing on events "PullRequestOpenedEvent", "PullRequestRescopedEvent" and "PullRequestReopenedEvent" and 
when a event is triggered it trigger the specified jenklins job with parameter of the SHA-1 commit id of the source repository

The Jenkins job need to take a least one parameter containing the source "SHA-1", this can be used to checkout 
the changed source set and process the job to your specific needs.

The "Disable automatic build" checkbox on the pull-request allow you to disable any trigger updates for the pull-request. This is useful if you have a pull-request that is updated often and you want to control when it should trigger a build.

The "Build" button on the pull-request allow you to force a immediately build of the pull-request. This can be used in combination with "Disable automatic build" to manual control when it should be build

This plug-in is created under the philosophy of open source that software should be free and accessible to all.

##  Installation
For installing the plug-in you need to open the "Administration" -> "Manage Add-ons" in Stash. Choose "Upload-add-on" and choose the
"jenkins-integration-plugin-<version>.jar" and upload. The plug-in is active by default so the next step is to setup the specific jenkins parameter.
 
Go to your repository and choose "settings" -> "Jenkins Pull Request integration" :

* "Trigger on new pull-request" - checked if it should trigger a build when a pull-request is created.
* "Trigger on update pull-request" - checked if it should trigger a build when source SHA-1 is updated.
* "Trigger on reopen pull-request" - checked if it should trigger a build when the pull-request is reopen
* "Url" - the base URL pointing to the job http://ci-server:8082/job/Single-Revision-Build/
* "Username" - if jenkins require authentication
* "Password" - if jenkins require authentication
* "Build ref. field" - The name of job parameter the source SHA-1 should be inserted in when a build is triggered
* "Build title field" - This is optional and the name of the job parameter the pull-request title should be set on when a build is triggered.
* "Delay build" - This is by default 300 seconds (5 min) and will deplay the trigger of the build. Useful if have a process where you update the pull-request often
* "Pull-Request URL field" - The is optional and point to the name of the job parameter that should contain the pull-request url. 
 
##  Building the source
For building the source it's required you install the [Atlassian Plugin SDK](https://developer.atlassian.com/display/DOCS/Set+up+the+Atlassian+Plugin+SDK+and+Build+a+Project) The easiest way is to follow the link
 
Building the source is pretty easy, just execute the maven build manager that is part of the "Atlassian Plugin SDK" "mvn install" compiles and build the "jenkins-integration-plugin" artifact.

##  Upgrade from 1.0.0 to 1.0.1
With version 1.0.1 there is now support for multiple repository settings. When you install the plug-in it's required to go to the plug-in settings
for upgrading. Go to your repository and choose "settings" -> "Jenkins Pull Request integration" and it will automatic upgrade the configuration

##  Upgrade from 1.0.1 to 1.0.2
Encrypt and Decrypt username and password based on the H/W MAC address. Because it use the MAC address users will have to update there
configuration if they change the network interface.

This is not a bullet proof way of encrypt a password, but it’s at least better than storing it as plain text or store the key together
with the password.

##  Upgrade from 1.0.2 to 1.0.3
Support for simple load balancing against multiple CI servers. The only requirement is that all servers share the same user and API token.
If one CI server fail to process the request it will try the next in the list until tried every server.

- Disable the feature it posted the job URL as part of the comment because it will point to the wrong job in most cases 

##  Upgrade from 1.0.3 to 1.0.4
- Added support for trigger a build manual from the pull-reques.
- Tested with stash 2.10.1.
- Fixed an issue with the list of build servers contains white spaces when it was loaded.

##  Upgrade from 1.0.4 to 1.0.5
- Added support for delay the trigger of a build
- The "build" button will unschedule the current build if any and force a new build immediately

##  Upgrade from 1.0.5 to 1.0.6
- Added support for passing the pull-request url to a job parameter to give full traceability both ways.

##  Upgrade from 1.0.6 to 1.0.7
- The scheduler is now using the latest changes when it run, instead of using the change set when it was scheduled.

##  Upgrade from 1.0.7 to 1.0.8
- Upgrade to Stash 3.5
- Introduce a setting to "Disable automatic build" by default when open or re-open a pull-request

Flemming Harms

[Follow : @fnharms](https://twitter.com/intent/user?screen_name=fnharms)