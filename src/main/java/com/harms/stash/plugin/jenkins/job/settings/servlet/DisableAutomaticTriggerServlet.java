package com.harms.stash.plugin.jenkins.job.settings.servlet;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

/**
 * This servlet disable or enable the automatic build trigger for the specified pull-request
 * @author fharms
 *
 */
public class DisableAutomaticTriggerServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(DisableAutomaticTriggerServlet.class);
    
    private static final long serialVersionUID = -6947257382708409328L;
    private final PluginSettings settings;
    private final RepositoryService repositoryService;

    
    public DisableAutomaticTriggerServlet(RepositoryService repositoryService,PluginSettingsFactory pluginSettingsFactory) {
        log.debug("invoked constructor");
        this.repositoryService = repositoryService;
        this.settings = pluginSettingsFactory.createGlobalSettings();
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] components = retrieveParameters(req);
        
        if (components.length < 3) {
            log.error(String.format("The DisableAutomaticTriggerServlet is invoked with the incorrect number of parameters %s", Arrays.toString(components)));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (req.getParameter("disableBuildParameter") != null) {
            Repository repository = repositoryService.getBySlug(components[1], components[2]);
            updatePullRequestSetttings(req.getParameter("disableBuildParameter"), repository.getProject().getKey(),repository.getSlug(),Long.valueOf(components[3]));
            resp.setStatus(HttpServletResponse.SC_OK);
        }
        
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] components = retrieveParameters(req);
        
        if (components.length < 4) {
            log.error(String.format("The DisableAutomaticTriggerServlet is invoked with the incorrect number of parameters %s", Arrays.toString(components)));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        Repository repository = repositoryService.getBySlug(components[1], components[2]);
        if (repository == null) {
            log.error(String.format("Error lookup the repository based on the parameters %s", Arrays.toString(components)));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        //handle settings for a pull-request
        String readPullRequestSettings = readPullRequestSettings(resp, new Long(components[3]), repository);
        if (readPullRequestSettings != null) {
            resp.setContentType("text/plain");
            resp.getWriter().print(readPullRequestSettings);
            resp.getWriter().flush();
        }
    }

    private String[] retrieveParameters(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        log.debug(String.format("invoked with path info %s", pathInfo));
        
        String[] components = pathInfo.split("/");
        log.debug(String.format("serlvet parameters %s", Arrays.toString(components)));
        return components;
    }

    private String readPullRequestSettings(HttpServletResponse resp, Long pullRequestId, Repository repository) throws IOException {
        return PluginSettingsHelper.isAutomaticBuildDisabled(repository.getProject().getKey(), repository.getSlug(), pullRequestId, settings) ? "CHECKED":"";
    }
    
    /**
     * Updates the pull-request settings with the state of the disable automatic check box
     * @param pluginSettings - {@link PluginSettings}
     * @param disableAutomaticBuildParameter - the state of the disable automatic check box
     * @param projectKey
     * @param slug
     * @param pullRequestId
     */
    private void updatePullRequestSetttings(String disableAutomaticBuildParameter,String projectKey, String slug, Long pullRequestId) {
        if (disableAutomaticBuildParameter.isEmpty()) {
            PluginSettingsHelper.clearAutomaticBuildFlag(projectKey,slug,pullRequestId,settings); 
        } else {
            PluginSettingsHelper.enableAutomaticBuildFlag(projectKey,slug,pullRequestId,settings); 
        }
    }
}
