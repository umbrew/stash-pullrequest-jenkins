package com.harms.stash.plugin.jenkins.job.intergration;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.event.api.EventListener;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.RunMode;
import com.atlassian.scheduler.config.Schedule;
import com.atlassian.stash.event.pull.PullRequestDeclinedEvent;
import com.atlassian.stash.event.pull.PullRequestEvent;
import com.atlassian.stash.event.pull.PullRequestMergedEvent;
import com.atlassian.stash.event.pull.PullRequestOpenedEvent;
import com.atlassian.stash.event.pull.PullRequestReopenedEvent;
import com.atlassian.stash.event.pull.PullRequestRescopedEvent;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.UserService;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

public class StashEventListener {
    private static final Logger log = LoggerFactory.getLogger(StashEventListener.class);
    
    private final PluginSettings settings;
    private final SchedulerService schedulerService;
    private final StashAuthenticationContext stashAuthContext;
    
    public StashEventListener(PluginSettingsFactory pluginSettingsFactory, JobTrigger jenkinsCiIntergration, SchedulerService pluginScheduler,StashAuthenticationContext stashAuthContext,SecurityService securityService, PullRequestService pullRequestService,UserService userService) {
        this.schedulerService = pluginScheduler;
        this.settings = pluginSettingsFactory.createGlobalSettings();
        this.stashAuthContext = stashAuthContext;
        pluginScheduler.registerJobRunner(JenkinsJobScheduler.jobRunnerKey, new JenkinsJobScheduler(pullRequestService,userService,securityService,jenkinsCiIntergration));
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
        
        boolean isDisableAutomaticBuildByDefault = PluginSettingsHelper.isDisableAutomaticBuildByDefault(prd.slug,settings);
        if (isDisableAutomaticBuildByDefault) {
        	PluginSettingsHelper.enableAutomaticBuildFlag(prd.projectKey, prd.slug, prd.pullRequestId, settings);
        	return;
        }
        
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
        
        boolean isDisableAutomaticBuildByDefault = PluginSettingsHelper.isDisableAutomaticBuildByDefault(prd.slug,settings);
        if (isDisableAutomaticBuildByDefault) {
        	PluginSettingsHelper.enableAutomaticBuildFlag(prd.projectKey, prd.slug, prd.pullRequestId, settings);
        	return;
        }
        
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
    	String scheduleJobKey = PluginSettingsHelper.getScheduleJobKey(prd.slug,prd.pullRequestId);
		final Calendar scheduleJobTime = PluginSettingsHelper.getScheduleJobTime(scheduleJobKey);
        if ((scheduleJobTime == null) || (System.currentTimeMillis() > scheduleJobTime.getTime().getTime())) {
            Map<String, Serializable> jobData = JenkinsJobScheduler.buildJobDataMap(pushEvent.getPullRequest(),stashAuthContext,getTriggerEventType(pushEvent));
            Date jobTime = PluginSettingsHelper.generateScheduleJobTime(prd.slug, settings, prd.pullRequestId);
            try {
    			schedulerService.scheduleJob( 
    					JobId.of(scheduleJobKey), 
    			        JobConfig.forJobRunnerKey(JenkinsJobScheduler.jobRunnerKey) 
    			                .withParameters(jobData)
    			                .withRunMode(RunMode.RUN_ONCE_PER_CLUSTER) 
    			                .withSchedule(Schedule.runOnce(jobTime)));
    		} catch (SchedulerServiceException e) {
    			log.error(String.format("Not able to schedule jenkins build with job id %s at %t",scheduleJobKey,jobTime));
    		}
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
        PluginSettingsHelper.resetScheduleTime(PluginSettingsHelper.getScheduleJobKey(pushEvent.getPullRequest().getFromRef().getRepository().getSlug(),pushEvent.getPullRequest().getId()));
        //make sure we clean up the disable automatic property 
        removeDisableAutomaticBuildProperty(pushEvent);
    }
    
    @EventListener
    public void mergePullRequest(PullRequestMergedEvent pushEvent)
    {
        PluginSettingsHelper.resetScheduleTime(PluginSettingsHelper.getScheduleJobKey(pushEvent.getPullRequest().getFromRef().getRepository().getSlug(),pushEvent.getPullRequest().getId()));
        //make sure we clean up the disable automatic property 
        removeDisableAutomaticBuildProperty(pushEvent);
    }
}
