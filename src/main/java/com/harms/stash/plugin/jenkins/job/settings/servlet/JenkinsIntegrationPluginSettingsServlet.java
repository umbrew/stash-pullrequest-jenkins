package com.harms.stash.plugin.jenkins.job.settings.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.google.common.collect.Maps;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.UpgradeService;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_1;

public class JenkinsIntegrationPluginSettingsServlet extends HttpServlet {
    private static final long serialVersionUID = -1645440333554544743L;
    
    private final SoyTemplateRenderer soyTemplateRenderer;
    private final RepositoryService repositoryService;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final UserManager userManager;
    private final UpgradeService upgradeService;


    public JenkinsIntegrationPluginSettingsServlet(SoyTemplateRenderer soyTemplateRenderer, RepositoryService repositoryService, PluginSettingsFactory pluginSettingsFactory, UserManager userService) {
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.repositoryService = repositoryService;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.userManager = userService;
        upgradeService = new UpgradeService(pluginSettingsFactory);
        
    }
    
    protected void render(HttpServletResponse resp, Map<String, Object> data) throws IOException, ServletException {
        String template = "stash.config.jenkins.job.integration.repositorySettings";
        resp.setContentType("text/html;charset=UTF-8");
        try {
            soyTemplateRenderer.render(resp.getWriter(),
                    "org.harms.stash.jenkins-integration-plugin:setting-soy",
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
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String pathInfo = req.getPathInfo();

        String[] components = pathInfo.split("/");

        if (components.length < 3) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        if (req.getParameter("disableBuildParameter") != null) {
            Repository repository = repositoryService.getBySlug(components[1], components[2]);
            updatePullRequestSetttings(pluginSettings, req.getParameter("disableBuildParameter"), repository.getProject().getKey(),repository.getSlug(),components[3]);
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
           updatePluginSettings(pluginSettings, req.getParameterMap());
           resp.sendRedirect(req.getRequestURI());
        }    
    }

   
    /**
     * Update plugin settings from the parameterMap
     * @param ps - {@link PluginSettings} 
     * @param parameterMap - A map containing parameter name and value 
     */
    private void updatePluginSettings(PluginSettings ps, Map<String, String[]> parameterMap) {
        String slug = parameterMap.get("repository.slug")[0];
        resetSettings(ps,slug);
        ps.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_BASE_URL,slug), parameterMap.get("jenkinsBaseUrl")[0]);
        ps.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_REF_FIELD,slug), parameterMap.get("buildRefField")[0]);
        
        if (parameterMap.containsKey("triggerBuildOnCreate")) {
            ps.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE,slug), "checked");
        }
        
        if (parameterMap.containsKey("triggerBuildOnUpdate")) {
            ps.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE,slug), "checked");
        }
        
        if (parameterMap.containsKey("triggerBuildOnReopen")) {
            ps.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN,slug), "checked");
        }
        
        if (!parameterMap.get("jenkinsUserName")[0].isEmpty()) {
            ps.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,slug), parameterMap.get("jenkinsUserName")[0]); 
        }
        if (!parameterMap.get("jenkinsPassword")[0].isEmpty()) {
            ps.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,slug), parameterMap.get("jenkinsPassword")[0]);
        }

        if (!parameterMap.get("buildTitleField")[0].isEmpty()) {
            ps.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,slug), parameterMap.get("buildTitleField")[0]);
        }
    }

    /**
     * Updates the pull-request settings with the state of the disable automatic check box
     * @param pluginSettings - {@link PluginSettings}
     * @param disableAutomaticBuildParameter - the state of the disable automatic check box
     * @param projectKey
     * @param slug
     * @param pullRequestId
     */
    private void updatePullRequestSetttings(PluginSettings pluginSettings, String disableAutomaticBuildParameter,String projectKey, String slug, String pullRequestId) {
        String key = PluginSettingsHelper.getDisableAutomaticBuildSettingsKey(projectKey,slug,pullRequestId);
        if (disableAutomaticBuildParameter.isEmpty()) {
            pluginSettings.remove(key); 
        } else {
            pluginSettings.put(key, "checked");
        }
        return;
    }

    /**
     * Clear the Plugin settings
     * @param pluginSettings
     * @param slug - Repository Key
     */
    private void resetSettings(PluginSettings pluginSettings, String slug) {
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,slug));
        
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN,slug));
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        //response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Get repoSlug from path
        String pathInfo = req.getPathInfo();
        String[] components = pathInfo.split("/");
        
        UserProfile user = userManager.getRemoteUser(req);
        if (user == null || (!userManager.isSystemAdmin(user.getUserKey()))) {
            redirectToLogin(req, resp);
        }
        
        if (components.length < 3) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        Repository repository = repositoryService.getBySlug(components[1], components[2]);
        if (repository == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        if (components.length == 4) {
            //handle settings for a pull-request
            String readPullRequestSettings = readPullRequestSettings(resp, components[3], repository);
            if (readPullRequestSettings != null) {
                resp.setContentType("text/plain");
                resp.getWriter().print(readPullRequestSettings);
                resp.getWriter().flush();
            }
        } else {
            Map<String, Object> context = Maps.newHashMap();
            upgradeSettings(repository); //run the upgrade steps for the settings
            resetFormFields(context);
            readPluginSettings(repository, context);
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
        upgradeService.process(); //if the upgrade is already executed this will just retun
    }

    private String readPullRequestSettings(HttpServletResponse resp, String pullRequestId, Repository repository) throws IOException {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String key = "stash.plugin.jenkins.settingsui." + repository.getProject().getKey() + "/" + repository.getSlug()+ "/" +pullRequestId;
        return (String) pluginSettings.get(key);
    }

    private void readPluginSettings(Repository repository, Map<String, Object> context) {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String slug = repository.getSlug();
        if (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_BASE_URL,slug)) != null){
            context.put("jenkinsBaseUrl", pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_BASE_URL,slug)));
        }
        
        if (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,slug)) != null){
            context.put("jenkinsUserName", pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,slug)));
        }
        
        if (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,slug)) != null){
            context.put("jenkinsPassword", pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,slug)));
        }
        
        if (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_REF_FIELD,slug)) != null){
            context.put("buildRefField", pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_REF_FIELD,slug)));
        }
        
        if (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,slug)) != null){
            context.put("buildTitleField", pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,slug)));
        }
        
        if (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE,slug)) != null){
            context.put("triggerBuildOnCreate", "checked=\"checked\"");
        } 
        
        if (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE,slug)) != null){
            context.put("triggerBuildOnUpdate", "checked=\"checked\"");
        } 
        
        if (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN,slug)) != null){
            context.put("triggerBuildOnReopen", "checked=\"checked\"");
        } 
        
        context.put("repository", repository);
    }

    private void resetFormFields(Map<String, Object> context) {
        context.put("jenkinsUserName", "");
        context.put("jenkinsPassword", "");
        context.put("buildRefField", "");
        context.put("buildTitleField", "");
        context.put("jenkinsBaseUrl", "");
        context.put("triggerBuildOnCreate", "");
        context.put("triggerBuildOnUpdate", "");
        context.put("triggerBuildOnReopen", "");
    }

}
