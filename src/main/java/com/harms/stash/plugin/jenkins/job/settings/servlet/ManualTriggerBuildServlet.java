package com.harms.stash.plugin.jenkins.job.settings.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.user.SecurityService;
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
    private final JobTrigger jenkinsCiIntergration;
    private final PullRequestService pullRequestService;
    private final PluginScheduler pluginScheduler;
    private final PluginSettings settings;
    
    private final SecurityService securityService;

    
    public ManualTriggerBuildServlet(JobTrigger jenkinsCiIntergration, PullRequestService pullRequestService, PluginScheduler pluginScheduler, PluginSettingsFactory pluginSettingsFactory,UserManager userManager,SecurityService securityService,LoginUriProvider loginUriProvider) {
        super(loginUriProvider, userManager);
        log.debug("invoked constructor");
        this.securityService = securityService;
        this.pluginScheduler = pluginScheduler;
        this.pullRequestService = pullRequestService;
        this.jenkinsCiIntergration = jenkinsCiIntergration;
        this.settings = pluginSettingsFactory.createGlobalSettings();
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
            try {
             pluginScheduler.unscheduleJob(scheduleJobKey); //Unscheduled the current job if any 
            } catch (IllegalArgumentException e) {
             log.debug("No current job was scheduled for job id "+scheduleJobKey);  
            }
            Map<String, Object> jobData = JenkinsJobScheduler.buildJobDataMap(pullRequest,jenkinsCiIntergration,pullRequestService,userManager,securityService,TriggerRequestEvent.FORCED_BUILD);
            pluginScheduler.scheduleJob(scheduleJobKey, JenkinsJobScheduler.class, jobData,PluginSettingsHelper.setScheduleJobTime(slug, settings, pullRequestId,0), 0);
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            log.error(String.format("Not able to retrieve the pull-reqeust based on the repository id %s and pull-request id %s",repositoryId,pullRequestId));
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Not able to retrieve the pull-reqeust based on the repository id %s and pull-request id %s",repositoryId,pullRequestId));
        }
    }
}
