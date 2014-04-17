package com.harms.stash.plugin.jenkins.job.settings.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.google.common.collect.Maps;
import com.harms.stash.plugin.jenkins.job.settings.JenkinsSettings;
import com.harms.stash.plugin.jenkins.job.settings.PxeSettings;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.UpgradeService;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_1;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_2;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_3;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_5;

public class JenkinsIntegrationPluginSettingsServlet extends HttpServlet {
    private static final long serialVersionUID = -1645440333554544743L;
    
    private static final Logger log = LoggerFactory.getLogger(JenkinsIntegrationPluginSettingsServlet.class);
    private final SoyTemplateRenderer soyTemplateRenderer;
    private final RepositoryService repositoryService;
    private final UserManager userManager;
    private final UpgradeService upgradeService;
    private final PxeSettings pxeSupportSettings;
    private final JenkinsSettings jenkinsSettings;
    private String slug;
    private String projectKey;

    public JenkinsIntegrationPluginSettingsServlet(SoyTemplateRenderer soyTemplateRenderer, RepositoryService repositoryService, PluginSettingsFactory pluginSettingsFactory, UserManager userService, PxeSettings pxeSupportSettings, JenkinsSettings jenkinsSettings) {
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.repositoryService = repositoryService;
        this.userManager = userService;
        this.pxeSupportSettings = pxeSupportSettings;
        this.jenkinsSettings = jenkinsSettings;
        upgradeService = new UpgradeService(pluginSettingsFactory);
        
    }
    
    protected void render(HttpServletResponse resp, Map<String, Object> data) throws IOException, ServletException {
        String template = "stash.config.jenkins.job.integration.repositorySettings";
        resp.setContentType("text/html;charset=UTF-8");
        try {
            soyTemplateRenderer.render(resp.getWriter(),
                    "com.harms.stash.jenkins-integration-plugin:setting-soy",
                    template,
                    data);
        } catch (SoyException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new ServletException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.debug("Invoke JenkinsIntegrationPluginSettingsServlet");
        if (retrieveParameters(req, resp)) {
            try {
                Map<String, String[]> parameterMap = req.getParameterMap();
                jenkinsSettings.updateSettings(parameterMap.get("repository.slug")[0], parameterMap);
                pxeSupportSettings.updateSettings(parameterMap.get("repository.slug")[0], parameterMap);
            } catch (Exception e) {
                throw new ServletException("Not able to update the settings", e);
            }
            resp.sendRedirect(req.getRequestURI());
        }
    }

    private boolean retrieveParameters(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        String[] components = pathInfo.split("/");
        if (components.length < 3) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        
        slug = components[2];
        projectKey = components[1];
        
        return true;
    }
    
    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        //response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        UserProfile user = userManager.getRemoteUser(req);
        if (user == null || (!userManager.isSystemAdmin(user.getUserKey()))) {
            redirectToLogin(req, resp);
        }
        if (retrieveParameters(req, resp)) {
            Repository repository = repositoryService.getBySlug(projectKey, slug);
            if (repository == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            Map<String, Object> context = Maps.newHashMap();
            upgradeSettings(repository); //run the upgrade steps for the settings
           
           
            try {
                readPluginSettings(repository, context);
            } catch (Exception e) {
                throw new ServletException("Not able to read the settings", e);
            }
            render(resp, context);
        }
    }

    
    /**
     * Add the list of upgrade steps and execute the upgrade process.
     * If upgrade steps is already executed this will be ignored
     * @param repository
     */
    private void upgradeSettings(Repository repository) {
        upgradeService.addUpgradeStep(new Upgrade_1_0_1(repository.getSlug()));
        upgradeService.addUpgradeStep(new Upgrade_1_0_2(repository.getSlug()));
        upgradeService.addUpgradeStep(new Upgrade_1_0_3(repository.getSlug()));
        upgradeService.addUpgradeStep(new Upgrade_1_0_5(repository.getSlug()));
        upgradeService.process(); //if the upgrade is already executed this will just return
    }

    private void readPluginSettings(Repository repository, Map<String, Object> context) throws Exception {
        String slug = repository.getSlug();
        resetFormFields(context);
        context.putAll(jenkinsSettings.readSettings(slug));
        context.putAll(pxeSupportSettings.readSettings(slug));
        context.put("repository", repository);
    }

    private void resetFormFields(Map<String, Object> context) {
        context.put("jenkinsUserName", "");
        context.put("jenkinsPassword", "");
        context.put("buildRefField", "");
        context.put("buildTitleField", "");
        context.put("jenkinsBaseUrl", "");
        context.put("jenkinsCIServerList",null);
        context.put("triggerBuildOnCreate", "");
        context.put("triggerBuildOnUpdate", "");
        context.put("triggerBuildOnReopen", "");
        context.put("buildPullRequestUrlField", "");
        context.put("buildDelayField", "");
        context.put("pxeHostUrl", "");
        context.put("hostCheckerUrl", "");
    }

}
