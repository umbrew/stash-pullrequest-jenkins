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

public class JenkinsIntegrationPluginSettingsServlet extends HttpServlet {
    private static final long serialVersionUID = -1645440333554544743L;
    
    private final SoyTemplateRenderer soyTemplateRenderer;
    private final RepositoryService repositoryService;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final UserManager userManager;

    public JenkinsIntegrationPluginSettingsServlet(SoyTemplateRenderer soyTemplateRenderer, RepositoryService repositoryService, PluginSettingsFactory pluginSettingsFactory, UserManager userService) {
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.repositoryService = repositoryService;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.userManager = userService;
        
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
     * @param pluginSettings - {@link PluginSettings} 
     * @param parameterMap - A map containing parameter name and value 
     */
    private void updatePluginSettings(PluginSettings pluginSettings, Map<String, String[]> parameterMap) {
        resetSettings(pluginSettings);

        pluginSettings.put(PluginSettingsHelper.JENKINS_BASE_URL, parameterMap.get("jenkinsBaseUrl")[0]);
        pluginSettings.put(PluginSettingsHelper.BUILD_REF_FIELD, parameterMap.get("buildRefField")[0]);
        
        if (parameterMap.containsKey("triggerBuildOnCreate")) {
            pluginSettings.put(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE, "checked");
        }
        
        if (parameterMap.containsKey("triggerBuildOnUpdate")) {
            pluginSettings.put(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE, "checked");
        }
        
        if (parameterMap.containsKey("triggerBuildOnReopen")) {
            pluginSettings.put(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN, "checked");
        }
        
        if (!parameterMap.get("jenkinsUserName")[0].isEmpty()) {
            pluginSettings.put(PluginSettingsHelper.JENKINS_USERNAME, parameterMap.get("jenkinsUserName")[0]); 
        }
        if (!parameterMap.get("jenkinsPassword")[0].isEmpty()) {
            pluginSettings.put(PluginSettingsHelper.JENKINS_PASSWORD, parameterMap.get("jenkinsPassword")[0]);
        }

        if (!parameterMap.get("buildTitleField")[0].isEmpty()) {
            pluginSettings.put(PluginSettingsHelper.BUILD_TITLE_FIELD, parameterMap.get("buildTitleField")[0]);
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
     */
    private void resetSettings(PluginSettings pluginSettings) {
        pluginSettings.remove(PluginSettingsHelper.JENKINS_USERNAME);
        pluginSettings.remove(PluginSettingsHelper.JENKINS_PASSWORD);
        pluginSettings.remove(PluginSettingsHelper.BUILD_TITLE_FIELD);
        
        pluginSettings.remove(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE);
        pluginSettings.remove(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE);
        pluginSettings.remove(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN);
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
            resetFormFields(context);
            readPluginSettings(repository, context);
            render(resp, context);
        }
    }

    private String readPullRequestSettings(HttpServletResponse resp, String pullRequestId, Repository repository) throws IOException {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String key = "stash.plugin.jenkins.settingsui." + repository.getProject().getKey() + "/" + repository.getSlug()+ "/" +pullRequestId;
        return (String) pluginSettings.get(key);
    }

    private void readPluginSettings(Repository repository, Map<String, Object> context) {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        if (pluginSettings.get(PluginSettingsHelper.JENKINS_BASE_URL) != null){
            context.put("jenkinsBaseUrl", pluginSettings.get(PluginSettingsHelper.JENKINS_BASE_URL));
        }
        
        if (pluginSettings.get(PluginSettingsHelper.JENKINS_USERNAME) != null){
            context.put("jenkinsUserName", pluginSettings.get(PluginSettingsHelper.JENKINS_USERNAME));
        }
        
        if (pluginSettings.get(PluginSettingsHelper.JENKINS_PASSWORD) != null){
            context.put("jenkinsPassword", pluginSettings.get(PluginSettingsHelper.JENKINS_PASSWORD));
        }
        
        if (pluginSettings.get(PluginSettingsHelper.BUILD_REF_FIELD) != null){
            context.put("buildRefField", pluginSettings.get(PluginSettingsHelper.BUILD_REF_FIELD));
        }
        
        if (pluginSettings.get(PluginSettingsHelper.BUILD_TITLE_FIELD) != null){
            context.put("buildTitleField", pluginSettings.get(PluginSettingsHelper.BUILD_TITLE_FIELD));
        }
        
        if (pluginSettings.get(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE) != null){
            context.put("triggerBuildOnCreate", "checked=\"checked\"");
        } 
        
        if (pluginSettings.get(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE) != null){
            context.put("triggerBuildOnUpdate", "checked=\"checked\"");
        } 
        
        if (pluginSettings.get(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN) != null){
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
