package com.harms.stash.plugin.jenkins.job.intergration;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.scheduling.PluginJob;
import com.atlassian.stash.pull.PullRequest;
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
        PullRequest pr = (PullRequest) jobDataMap.get(PullRequest.class.getName());
        String jobKey = PluginSettingsHelper.getScheduleJobKey(pr.getFromRef().getRepository().getSlug(),pr.getId());
        JenkinsJobTrigger jenkinsCI = (JenkinsJobTrigger) jobDataMap.get(JenkinsJobTrigger.class.getName());
        TriggerRequestEvent eventType = (TriggerRequestEvent) jobDataMap.get(TriggerRequestEvent.class.getName());
        String userName = (String) jobDataMap.get("UserName");
        SecurityService securityService = (SecurityService) jobDataMap.get(SecurityService.class.getName());
        
        try {
            securityService.doAsUser("background_trigger_jenkins_job", userName, new UserOperation(jenkinsCI, pr, eventType));
        } catch (Throwable e) {
            log.error(String.format("Not able to execute the background job as user %s",userName, e));
        } finally {
            PluginSettingsHelper.resetScheduleTime(jobKey);
        }
    }
    
    private class UserOperation implements Operation<Object, Throwable> {

        private final TriggerRequestEvent eventType;
        private final JenkinsJobTrigger jenkinsCI;
        private final PullRequest pr;

        public UserOperation(JenkinsJobTrigger jenkinsCI, PullRequest pr, TriggerRequestEvent eventType) {
            this.jenkinsCI = jenkinsCI;
            this.pr = pr;
            this.eventType = eventType;
        }
        
        @Override
        public Object perform() throws Throwable {
            PullRequestData prd = new PullRequestData(pr);
            String jenkinsBaseUrl = jenkinsCI.nextCIServer(prd.slug);
            if (jenkinsCI.validateSettings(jenkinsBaseUrl,prd.slug)) {
                log.debug(String.format("trigger build with parameter (%s, %s, %s, %s, %s, %s,%s",prd.repositoryId, prd.latestChanges, prd.pullRequestId,prd.title,prd.slug,eventType,jenkinsBaseUrl));
                jenkinsCI.triggerBuild(prd.repositoryId, prd.latestChanges, prd.pullRequestId,prd.title,prd.slug,eventType, 0,jenkinsBaseUrl);
            } else {
                log.warn("Jenkins base URL & Build reference field is missing, please add the information in the pull-in settings");
            }
            return null;
        }
        
    }

}
