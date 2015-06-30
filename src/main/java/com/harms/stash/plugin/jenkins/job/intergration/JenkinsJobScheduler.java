package com.harms.stash.plugin.jenkins.job.intergration;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.scheduling.PluginJob;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.user.SecurityService;
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
public class JenkinsJobScheduler implements PluginJob {
    private static final Logger log = LoggerFactory.getLogger(JenkinsJobScheduler.class);

    @Override
    public void execute(Map<String, Object> jobDataMap) {
        PullRequestService pullrequestService = (PullRequestService) jobDataMap.get("pullRequestService");
        Long pullRequestId = (Long) jobDataMap.get("pullrequest_id");
        Integer repositoryId = (Integer) jobDataMap.get("repository_id");
        String slug = (String) jobDataMap.get("slug");
        JenkinsJobTrigger jenkinsCI = (JenkinsJobTrigger) jobDataMap.get("JobTrigger");
        TriggerRequestEvent eventType = (TriggerRequestEvent) jobDataMap.get("TriggerRequestEvent");
        String userName = (String) jobDataMap.get("UserName");
        SecurityService securityService = (SecurityService) jobDataMap.get("SecurityService");
        String jobKey = PluginSettingsHelper.getScheduleJobKey(slug,pullRequestId);

        try {
            securityService.doAsUser("background_trigger_jenkins_job", userName, new UserOperation(pullrequestService,jenkinsCI, pullRequestId, repositoryId, eventType));
        } catch (Throwable e) {
            log.error(String.format("Not able to execute the background job as user %s",userName, e));
        } finally {
            PluginSettingsHelper.resetScheduleTime(jobKey);
        }

    }

    /**
     * Build a map of job data to be passed to the {@link JenkinsJobScheduler}
     * @param pr - The {@link PullRequest}
     * @param jenkinsCiIntergration - The {@link JenkinsJobTrigger}
     * @param pullRequestService - The {@link PullRequestService}
     * @param userManager - The {@link UserManager}
     * @param securityService - Then {@link SecurityService}
     * @param event - The type of {@link TriggerRequestEvent}
     * @return A map with the job data
     */
    static public Map<String, Object> buildJobDataMap(PullRequest pr, JobTrigger jenkinsCiIntergration, PullRequestService pullRequestService, UserManager userManager, SecurityService securityService, TriggerRequestEvent event) {
        Map<String, Object> jobDataMap = new HashMap<String, Object>();
        jobDataMap.put("JobTrigger", jenkinsCiIntergration);
        jobDataMap.put("pullRequestService", pullRequestService);
        jobDataMap.put("pullrequest_id", pr.getId());
        jobDataMap.put("repository_id", pr.getFromRef().getRepository().getId());
        jobDataMap.put("slug", pr.getFromRef().getRepository().getSlug());
        jobDataMap.put("TriggerRequestEvent", event);
        jobDataMap.put("UserName",userManager.getRemoteUser().getUsername());
        jobDataMap.put("SecurityService",securityService);
        return jobDataMap;
    }

    private class UserOperation implements Operation<Object, Throwable> {

        private final TriggerRequestEvent eventType;
        private final JenkinsJobTrigger jenkinsCI;
        private final Long pullRequestId;
        private final Integer repositoryId;
        private final PullRequestService pullrequestService;

        public UserOperation(PullRequestService pullrequestService, JenkinsJobTrigger jenkinsCI, Long pullRequestId, Integer repositoryId, TriggerRequestEvent eventType) {
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
                    jenkinsCI.triggerBuild(prd.repositoryId, prd.latestChanges, prd.pullRequestId,prd.title,prd.slug,eventType, 0,jenkinsBaseUrl, prd.projectKey, prd.fromBranchId, prd.toBranchId);
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
