package com.harms.stash.plugin.jenkins.job.intergration;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.event.api.EventListener;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.stash.event.pull.PullRequestDeclinedEvent;
import com.atlassian.stash.event.pull.PullRequestEvent;
import com.atlassian.stash.event.pull.PullRequestMergedEvent;
import com.atlassian.stash.event.pull.PullRequestOpenedEvent;
import com.atlassian.stash.event.pull.PullRequestReopenedEvent;
import com.atlassian.stash.event.pull.PullRequestRescopedEvent;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.user.SecurityService;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

public class StashEventListener {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(StashEventListener.class);
    
    private final JobTrigger jenkinsCI;
    private final PluginSettings settings;
    private final PluginScheduler pluginScheduler;
    private final UserManager userManager;
    private final SecurityService securityService;
    
    public StashEventListener(PluginSettingsFactory pluginSettingsFactory, JobTrigger jenkinsCiIntergration, PluginScheduler pluginScheduler,UserManager userManager,SecurityService securityService) {
        this.pluginScheduler = pluginScheduler;
        this.settings = pluginSettingsFactory.createGlobalSettings();
        this.jenkinsCI = jenkinsCiIntergration;
        this.userManager = userManager;
        this.securityService = securityService;
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
        
        boolean triggerBuildOnCreate = PluginSettingsHelper.isTriggerBuildOnCreate(prd.slug,settings);
        if (triggerBuildOnCreate) {
            scheduleJobTrigger(pushEvent, prd);
        }
    }
    
    @EventListener
    public void updatePullRequest(PullRequestRescopedEvent pushEvent)
    {
        PullRequestData prd = new PullRequestData(pushEvent.getPullRequest());
        
        boolean isSourceChanged = !pushEvent.getPullRequest().getFromRef().getLatestChangeset().equals(pushEvent.getPreviousFromHash());
        boolean automaticBuildDisabled = PluginSettingsHelper.isAutomaticBuildDisabled(prd.projectKey,prd.slug,prd.pullRequestId,settings);
        
        boolean triggerBuildOnUpdate = PluginSettingsHelper.isTriggerBuildOnUpdate(prd.slug,settings);
        if ((triggerBuildOnUpdate) && (!automaticBuildDisabled) && (isSourceChanged)) {
            scheduleJobTrigger(pushEvent, prd);
        }
    }

    @EventListener
    public void reopenPullRequest(PullRequestReopenedEvent pushEvent)
    {
        PullRequestData prd = new PullRequestData(pushEvent.getPullRequest());
        
        boolean automaticBuildDisabled = PluginSettingsHelper.isAutomaticBuildDisabled(prd.projectKey,prd.slug,prd.pullRequestId,settings);
        
        boolean triggerBuildOnReopen = PluginSettingsHelper.isTriggerBuildOnReopen(prd.slug,settings);
        if (triggerBuildOnReopen && !automaticBuildDisabled) {
            scheduleJobTrigger(pushEvent, prd);
        }
    }

    /**
     * Schedule a job trigger if a job is not already scheduled.
     * @param pushEvent
     * @param prd
     */
    private void scheduleJobTrigger(PullRequestEvent pushEvent, PullRequestData prd) {
        if (PluginSettingsHelper.getScheduleJobTime(PluginSettingsHelper.getScheduleJobKey(prd.slug,prd.pullRequestId)) == null) {
            pluginScheduler.scheduleJob(PluginSettingsHelper.getScheduleJobKey(prd.slug,prd.pullRequestId), JenkinsJobScheduler.class, createJobData(pushEvent), PluginSettingsHelper.generateScheduleJobTime(prd.slug, settings, prd.pullRequestId), 0);
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
    
    private Map<String, Object> createJobData(PullRequestEvent pushEvent) {
        Map<String, Object> jobDataMap = new HashMap<String, Object>();
        jobDataMap.put(JenkinsJobTrigger.class.getName(), jenkinsCI);
        jobDataMap.put(PullRequest.class.getName(), pushEvent.getPullRequest());
        jobDataMap.put(TriggerRequestEvent.class.getName(), getTriggerEventType(pushEvent));
        jobDataMap.put("UserName",userManager.getRemoteUser().getUsername());
        jobDataMap.put(SecurityService.class.getName(),securityService);
        return jobDataMap;
    }
}
