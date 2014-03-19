package com.harms.stash.plugin.jenkins.job.intergration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.event.api.EventListener;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.stash.event.pull.PullRequestDeclinedEvent;
import com.atlassian.stash.event.pull.PullRequestEvent;
import com.atlassian.stash.event.pull.PullRequestMergedEvent;
import com.atlassian.stash.event.pull.PullRequestOpenedEvent;
import com.atlassian.stash.event.pull.PullRequestReopenedEvent;
import com.atlassian.stash.event.pull.PullRequestRescopedEvent;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.repository.Repository;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

public class StashEventListener {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(StashEventListener.class);
    
    private final JobTrigger jenkinsCI;
    private final PluginSettings settings;
    
    public StashEventListener(PluginSettingsFactory pluginSettingsFactory, JobTrigger jenkinsCiIntergration) {
        this.settings = pluginSettingsFactory.createGlobalSettings();
        this.jenkinsCI = jenkinsCiIntergration;
    }
    
    /**
     * Return the Jenkins Trigger Job event type from the {@link PullRequestEvent}
     * @param pushEvent - The {@link PullRequest} eventtype 
     * @return a {@link TriggerRequestEvent}
     */
    private TriggerRequestEvent getTriggerEventType(PullRequestEvent pushEvent) {
        TriggerRequestEvent eventType = TriggerRequestEvent.PULLREQUEST_EVENT_CREATED;
        if (pushEvent instanceof PullRequestRescopedEvent) {
           eventType = TriggerRequestEvent.PULLREQUEST_EVENT_SOURCE_UPDATED;
        } else if (pushEvent instanceof PullRequestReopenedEvent) {
           eventType = TriggerRequestEvent.PULLREQUEST_EVENT_REOPEN;
        }
        return eventType;
    }
    
    @EventListener
    public void openPullRequest(PullRequestOpenedEvent pushEvent)
    {
        PullRequestData prd = new PullRequestData(pushEvent.getPullRequest());
        int retryCount = 0;
        
        String jenkinsBaseUrl = jenkinsCI.nextCIServer(prd.slug);
        boolean triggerBuildOnCreate = PluginSettingsHelper.isTriggerBuildOnCreate(prd.slug,settings);
        if ((triggerBuildOnCreate) && jenkinsCI.validateSettings(jenkinsBaseUrl,prd.slug)) {
            TriggerRequestEvent eventType = getTriggerEventType(pushEvent);
            jenkinsCI.triggerBuild(prd.repositoryId, prd.latestChanges, prd.pullRequestId,prd.title,prd.slug,eventType, retryCount,jenkinsBaseUrl);
        }
    }
    
    @EventListener
    public void updatePullRequest(PullRequestRescopedEvent pushEvent)
    {
        PullRequestData prd = new PullRequestData(pushEvent.getPullRequest());
        int retryCount = 0;
        String jenkinsBaseUrl = jenkinsCI.nextCIServer(prd.slug);
        
        boolean isSourceChanged = !pushEvent.getPullRequest().getFromRef().getLatestChangeset().equals(pushEvent.getPreviousFromHash());
        boolean automaticBuildDisabled = PluginSettingsHelper.isAutomaticBuildDisabled(prd.projectKey,prd.slug,prd.pullRequestId,settings);
        
        boolean triggerBuildOnUpdate = PluginSettingsHelper.isTriggerBuildOnUpdate(prd.slug,settings);
        if ((triggerBuildOnUpdate) && (!automaticBuildDisabled) && (jenkinsCI.validateSettings(jenkinsBaseUrl,prd.slug)) && (isSourceChanged)) {
            TriggerRequestEvent eventType = getTriggerEventType(pushEvent);
            jenkinsCI.triggerBuild(prd.repositoryId, prd.latestChanges, prd.pullRequestId,prd.title,prd.slug,eventType, retryCount,jenkinsBaseUrl);
  
        }
    }
    
    @EventListener
    public void reopenPullRequest(PullRequestReopenedEvent pushEvent)
    {
        PullRequestData prd = new PullRequestData(pushEvent.getPullRequest());
        int retryCount = 0;
        
        String jenkinsBaseUrl = jenkinsCI.nextCIServer(prd.slug);
        boolean automaticBuildDisabled = PluginSettingsHelper.isAutomaticBuildDisabled(prd.projectKey,prd.slug,prd.pullRequestId,settings);
        
        boolean triggerBuildOnReopen = PluginSettingsHelper.isTriggerBuildOnReopen(prd.slug,settings);
        if (triggerBuildOnReopen && !automaticBuildDisabled) {
            TriggerRequestEvent eventType = getTriggerEventType(pushEvent);
            jenkinsCI.triggerBuild(prd.repositoryId, prd.latestChanges, prd.pullRequestId,prd.title,prd.slug,eventType, retryCount,jenkinsBaseUrl);
 
            
        }
    }
    
    
    
    /**
     * Remove the disable automatic build settins when the pull-request is merged or declined
     * @param pushEvent
     */
    private void removeDisableAutomaticBuildProperty(PullRequestEvent pushEvent) {
        PullRequest pullRequest = pushEvent.getPullRequest();
        Repository repository = pullRequest.getToRef().getRepository();
        PluginSettingsHelper.clearAutomaticBuildFlag(repository.getProject().getKey(),repository.getSlug(),pullRequest.getId(),settings);
    }

    @EventListener
    public void declinedPullRequest(PullRequestDeclinedEvent pushEvent)
    {
        //make sure we clean up the disable automatic property 
        removeDisableAutomaticBuildProperty(pushEvent);
    }
    
    @EventListener
    public void mergePullRequest(PullRequestMergedEvent pushEvent)
    {
        //make sure we clean up the disable automatic property 
        removeDisableAutomaticBuildProperty(pushEvent);
    }
}
