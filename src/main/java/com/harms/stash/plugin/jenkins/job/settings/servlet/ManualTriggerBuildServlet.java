package com.harms.stash.plugin.jenkins.job.settings.servlet;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.RunMode;
import com.atlassian.scheduler.config.Schedule;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.UserService;
import com.harms.stash.plugin.jenkins.job.intergration.JenkinsJobScheduler;
import com.harms.stash.plugin.jenkins.job.intergration.JobTrigger;
import com.harms.stash.plugin.jenkins.job.intergration.TriggerRequestEvent;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

/**
 * This servlet is able to manual trigger a job based on the repository id and pull request id
 * 
 *  To invoke : call the servlet with following path info /repository-id/pull-request-id/
 * @author fharms
 *
 */
public class ManualTriggerBuildServlet extends JenkinsStashBaseServlet {
    private static final Logger log = LoggerFactory.getLogger(ManualTriggerBuildServlet.class);
    
    private static final long serialVersionUID = -6947257382708409328L;
    private final PullRequestService pullRequestService;
    private final SchedulerService schedulerService;
    private final PluginSettings settings;

    
    public ManualTriggerBuildServlet(JobTrigger jenkinsCiIntergration, PullRequestService pullRequestService, SchedulerService schedulerService, PluginSettingsFactory pluginSettingsFactory,StashAuthenticationContext stashAuthContext,SecurityService securityService,LoginUriProvider loginUriProvider, UserService userService) {
        super(loginUriProvider, stashAuthContext);
        log.debug("invoked constructor");
        this.schedulerService = schedulerService;
        this.pullRequestService = pullRequestService;
        this.settings = pluginSettingsFactory.createGlobalSettings();
        
        schedulerService.registerJobRunner(JenkinsJobScheduler.jobRunnerKey, new JenkinsJobScheduler(pullRequestService,userService,securityService,jenkinsCiIntergration));
        
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	    // Set to expire far in the past.
	    resp.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
	    // Set standard HTTP/1.1 no-cache headers.
	    resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
	    // Set IE extended HTTP/1.1 no-cache headers (use addHeader).
	    resp.addHeader("Cache-Control", "post-check=0, pre-check=0");
	    // Set standard HTTP/1.0 no-cache header.
	    resp.setHeader("Pragma", "no-cache");
    	
    	String pathInfo = req.getPathInfo();
        log.debug(String.format("invoked with path info %s", pathInfo));

        String[] components = pathInfo.split("/");

        log.debug(String.format("serlvet parameters %s", Arrays.toString(components)));
        
        if (components.length != 4) {
            log.error(String.format("The ManualTriggerBuildServlet is invoked with the incorrect number of parameters %s", Arrays.toString(components)));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        Long pullRequestId = new Long(components[3]);
        Integer repositoryId = new Integer(components[1]);
        
        PullRequest pullRequest = pullRequestService.getById(repositoryId, pullRequestId);
        if (pullRequest != null) {
            log.debug(String.format("Retrieved pull request information for %s", pullRequest.getId()));
            String slug = pullRequest.getFromRef().getRepository().getSlug();
            String scheduleJobKey = PluginSettingsHelper.getScheduleJobKey(slug,pullRequestId);
            JobId jobKey = JobId.of(scheduleJobKey);
            
            unscheduleJob(jobKey);
            
            Map<String, Serializable> jobData = JenkinsJobScheduler.buildJobDataMap(pullRequest,stashAuthContext,TriggerRequestEvent.FORCED_BUILD);
            Date jobTime = PluginSettingsHelper.setScheduleJobTime(slug, settings, pullRequestId,30);
            try {
				scheduleJob(pullRequestId, slug, scheduleJobKey, jobKey,jobData, jobTime);
				resp.setStatus(HttpServletResponse.SC_OK);
			} catch (SchedulerServiceException e) {
			    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Not able to schedule jenkins build with job id %1$s at %2$tH:%2$tM:%2$tS",scheduleJobKey,jobTime));
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
            
        } else {
            String errorMsg = String.format("Not able to retrieve the pull-reqeust based on the repository id %s and pull-request id %s",repositoryId,pullRequestId);
			log.error(errorMsg);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

	private void scheduleJob(Long pullRequestId,String slug, String scheduleJobKey, JobId jobKey,Map<String, Serializable> jobData, Date jobTime) throws IOException, SchedulerServiceException {
		try {
			schedulerService.scheduleJob( 
			        jobKey, 
			        JobConfig.forJobRunnerKey(JenkinsJobScheduler.jobRunnerKey) 
			                .withParameters(jobData)
			                .withRunMode(RunMode.RUN_ONCE_PER_CLUSTER) 
			                .withSchedule(Schedule.runOnce(jobTime)));
		} catch (SchedulerServiceException e) {
			log.error(String.format("Not able to schedule jenkins build with job id %1$s at %2$tH:%2$tM:%2$tS",scheduleJobKey,jobTime),e);
			throw e;
		}
	}

	private void unscheduleJob(JobId jobKey) {
		try {
			schedulerService.unscheduleJob(jobKey); //Unscheduled the current job if any 
		} catch (IllegalArgumentException e) {
			log.debug("No current job was scheduled for job id "+jobKey);  
		}
	}
}
