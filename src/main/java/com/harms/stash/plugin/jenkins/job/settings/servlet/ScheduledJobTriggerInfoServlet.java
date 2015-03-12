package com.harms.stash.plugin.jenkins.job.settings.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

/**
 * Retrieve the scheduled job trigger time if any. 
 * If a trigger is scheduled it will return the time in HH:mm:ss and
 * if no schedule time it will return an empty string.
 * 
 * To invoke : call the servlet with following path info /slug-id/pull-request-id/
 * 
 * @author fharms
 *
 */
public class ScheduledJobTriggerInfoServlet extends JenkinsStashBaseServlet {
    private static final long serialVersionUID = 604820129001885579L;
    private static final Logger log = LoggerFactory.getLogger(ScheduledJobTriggerInfoServlet.class);
    
    public ScheduledJobTriggerInfoServlet(LoginUriProvider loginUriProvider, StashAuthenticationContext stashAuthContext) {
     super(loginUriProvider, stashAuthContext);
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        log.debug(String.format("invoked with path info %s", pathInfo));

        String[] components = pathInfo.split("/");

        log.debug(String.format("serlvet parameters %s", Arrays.toString(components)));
        
        if (components.length != 3) {
            log.error(String.format("The ScheduledJobsServlet invoked with the incorrect number of parameters %s", Arrays.toString(components)));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String slug = components[1];
        Long pullRequestId = new Long(components[2]);

        Calendar jobTime = PluginSettingsHelper.getScheduleJobTime(PluginSettingsHelper.getScheduleJobKey(slug,pullRequestId));
        
        String formatTime = "";
        
        if (jobTime != null) {
         SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss aaa",req.getLocale());
         formatTime = sdf.format(jobTime.getTime());
         log.debug("Calculated execution time in "+formatTime);
        }
        
        resp.setContentType("text/plain");
        resp.getWriter().print(formatTime);
        resp.getWriter().flush();
    }

}
