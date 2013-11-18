# Jenkins Pull-Request Integration

This started as project "how difficult could it be" to create a integration between Stash and Jenkins CI. 
So I decided to build a small plugin for Stash inspired by all the examples provided by Atlassian, 
it's still working progress and it only tested with Stash 2.5.1 and Jenkins CI 1.509.2

This stash plugin hook into create/update/reopen pull-request to trigger a build on Jenkins CI through the API. 
3 hooks listing on events "PullRequestOpenedEvent", "PullRequestRescopedEvent" and "PullRequestReopenedEvent" and 
when a event is triggered it trigger the specified jenklins job with parameter of the SHA-1 of the source repository

The Jenkins job need to take a least one parameter containing the source "SHA-1", this can be used to checkout 
the changed source set and process the job to your specific needs.

This plug-in is created under the philosophy of open source that software should be free and accessible to all. 

##  Installation
For installing the plug-in you need to open the "Administration" -> "Manage Add-ons" in Stash. Choose "Upload-add-on" and choose the
"jenkins-integration-plugin-<version>.jar" and upload. The plug-in is active by default so the next step is to setup the specific jenkins parameter.
 
Go to your repository and choose "settings" -> "Jenkins Pull Request integration" :

* "Trigger on new pull-request" - checked if it should trigger a build when a pull-request is created.
* "Trigger on update pull-request" - checked if it should trigger a build when source SHA-1 is updated.
* "Trigger on reopen pull-request" - checked if it should trigger a build when the pull-request is reopen
* "Url" - the base URL pointing to the job http://softbuild:8082/job/Single-Revision-Build/
* "Username" - if jenkins require authentication
* "Password" - if jenkins require authentication
* "Build ref. field" - The name of job parameter the source SHA-1 should be set on when a build is triggered
* "Build title field" - This is optional and the name of the job parameter the pull-request title should be set on when a build is triggered.
 
##  Building the source
For building the source it's required you install the [Atlassian Plugin SDK](https://developer.atlassian.com/display/DOCS/Set+up+the+Atlassian+Plugin+SDK+and+Build+a+Project) The easiest way is to follow the link
 
Building the source is pretty easy, just execute the maven build manager that is part of the "Atlassian Plugin SDK" "mvn install" compiles and build the "jenkins-integration-plugin" artifact.

##  Upgrade from 1.0.0 to 1.0.1
With version 1.0.1 there is now support for multiple repository settings. When you install the plug-in it's required to go to the plug-in settings
for upgrading. Go to your repository and choose "settings" -> "Jenkins Pull Request integration" and it will automatic upgrade the configuration

##  Thinks on my todo list

* Support for adding Jenkins API token
* Automatic refresh the pull-request page when it's updated with job information

 
 