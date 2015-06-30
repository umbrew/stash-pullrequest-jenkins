package com.harms.stash.plugin.jenkins.job.intergration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import com.atlassian.stash.user.UserService;
import com.atlassian.stash.util.Operation;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

/**
 * A Job executor for trigger the jenkins job when the background job is executed
 * The triggerBuild code run inside a doAsUser to make sure it called with the correct
 * user and authorization, this is required otherwise it will give not authorized when
 * it try to update the pull-request
 * 
 * @author fharms
 *
 */
public class JenkinsJobScheduler implements JobRunner {
	public static final  JobRunnerKey jobRunnerKey = JobRunnerKey.of("com.harms.stash.plugin.jenkins.job.intergration:jenkins-job-intergration:ScheduleBuild");
    private static final Logger log = LoggerFactory.getLogger(JenkinsJobScheduler.class);
	private final PullRequestService pullRequestService;
	private final SecurityService securityService;
	private final UserService userService;
	private JobTrigger jenkinsCI;
		
    
    public JenkinsJobScheduler(PullRequestService pullRequestService, UserService userService,SecurityService securityService, JobTrigger jenkinsCiIntergration) {
		this.pullRequestService = pullRequestService;
		this.userService = userService;
		this.securityService = securityService;
		this.jenkinsCI = jenkinsCiIntergration;
	}
    @Override
	public JobRunnerResponse runJob(JobRunnerRequest request) {
    	try {
	    	 Map<String, Serializable> jobDataMap = request.getJobConfig().getParameters();
	         Long pullRequestId = (Long) jobDataMap.get("pullrequest_id");
	         Integer repositoryId = (Integer) jobDataMap.get("repository_id");
	         String slug = (String) jobDataMap.get("slug");
	         TriggerRequestEvent eventType = (TriggerRequestEvent) jobDataMap.get("TriggerRequestEvent");
	         StashUser user = userService.getUserByName((String) jobDataMap.get("User"));
	         String jobKey = PluginSettingsHelper.getScheduleJobKey(slug,pullRequestId);
	         
	         try {
	         	securityService.impersonating(user, "background_trigger_jenkins_job").call(new UserOperation(pullRequestService, jenkinsCI, pullRequestId, repositoryId, eventType));
	         } catch (Throwable e) {
	             log.error(String.format("Not able to execute the background job as user %s",user.getDisplayName(), e));
	         } finally {
	             PluginSettingsHelper.resetScheduleTime(jobKey);
	         }
    	} catch (Exception e) {
    		log.error(String.format("Not able to run job with id %s",request.getJobId().toString()),e);
    		return JobRunnerResponse.failed(e);
    	}
		return JobRunnerResponse.success();
	}

    /**
     * Build a map of job data to be passed to the {@link JenkinsJobScheduler}
     * @param pr - The {@link PullRequest}
     * @param event - The type of {@link TriggerRequestEvent}
     * @param pullRequestService - The {@link PullRequestService}
     * @param userManager - The {@link UserManager}
     * @param securityService - Then {@link SecurityService}
     * @return A map with the job data
     */
    static public Map<String, Serializable> buildJobDataMap(PullRequest pr, StashAuthenticationContext stashAuthenticationContext, TriggerRequestEvent event) {
        Map<String, Serializable> jobDataMap = new HashMap<String, Serializable>();
        jobDataMap.put("pullrequest_id", pr.getId());
        jobDataMap.put("repository_id", pr.getFromRef().getRepository().getId());
        jobDataMap.put("slug", pr.getFromRef().getRepository().getSlug());
        jobDataMap.put("TriggerRequestEvent", event);
        jobDataMap.put("User",stashAuthenticationContext.getCurrentUser().getName());
        return jobDataMap;
    }
    
    private class UserOperation implements Operation<Object, Throwable> {

        private final TriggerRequestEvent eventType;
        private final JobTrigger jenkinsCI;
        private final Long pullRequestId;
        private final Integer repositoryId;
        private final PullRequestService pullrequestService;

        public UserOperation(PullRequestService pullrequestService, JobTrigger jenkinsCI, Long pullRequestId, Integer repositoryId, TriggerRequestEvent eventType) {
            this.pullrequestService = pullrequestService;
            this.pullRequestId = pullRequestId;
            this.repositoryId = repositoryId;
            this.jenkinsCI = jenkinsCI;
            this.eventType = eventType;
        }

        @Override
        public Object perform() throws Throwable {
            PullRequest pr = pullrequestService.getById(repositoryId, pullRequestId); //this make sure we always working on the latest change set
            if (pr != null) {
                PullRequestData prd = new PullRequestData(pr);
                String jenkinsBaseUrl = jenkinsCI.nextCIServer(prd.slug);
                if (jenkinsCI.validateSettings(jenkinsBaseUrl,prd.slug)) {
                    log.debug(String.format("trigger build with parameter (%s, %s, %s, %s, %s, %s,%s",prd.repositoryId, prd.latestChanges, prd.pullRequestId,prd.title,prd.slug,eventType,jenkinsBaseUrl));
                    jenkinsCI.triggerBuild(prd.repositoryId, prd.latestChanges, prd.pullRequestId,prd.title,prd.slug,eventType, 0,jenkinsBaseUrl, prd.projectKey);
                } else {
                    log.warn("Jenkins base URL & Build reference field is missing, please add the information in the pull-in settings");
                }
            } else {
                log.warn(String.format("No able to retrieve the pull-request with the key repository id (%s) and pull-request id (%s)", repositoryId,pullRequestId));
            } 
            return null;
        }
        
    }

}
