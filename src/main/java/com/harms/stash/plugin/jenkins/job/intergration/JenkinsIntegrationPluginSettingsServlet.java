package com.harms.stash.plugin.jenkins.job.intergration;

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

public class JenkinsIntegrationPluginSettingsServlet extends HttpServlet {
    private static final long serialVersionUID = -1645440333554544743L;
    private static final String PLUGIN_STORAGE_KEY = "stash.plugin.jenkins.settingsui";
    private static final String BUILD_TITLE_FIELD = PLUGIN_STORAGE_KEY + ".buildTitleField";
    private static final String BUILD_REF_FIELD = PLUGIN_STORAGE_KEY + ".buildRefField";
    private static final String JENKINS_PASSWORD = PLUGIN_STORAGE_KEY + ".jenkinsPassword";
    private static final String JENKINS_USERNAME = PLUGIN_STORAGE_KEY + ".jenkinsUserName";
    private static final String JENKINS_BASE_URL = PLUGIN_STORAGE_KEY + ".jenkinsBaseUrl";
    private static final String TRIGGER_BUILD_ON_CREATE = PLUGIN_STORAGE_KEY + ".triggerBuildOnCreate";
    private static final String TRIGGER_BUILD_ON_UPDATE = PLUGIN_STORAGE_KEY + ".triggerBuildOnUpdate";
    private static final String TRIGGER_BUILD_ON_REOPEN = PLUGIN_STORAGE_KEY + ".triggerBuildOnReopen";
    
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

    protected void render(HttpServletResponse resp, String templateName, Map<String, Object> data) throws IOException, ServletException {
        resp.setContentType("text/html;charset=UTF-8");
        try {
            soyTemplateRenderer.render(resp.getWriter(),
                    "org.harms.stash.jenkins-integration-plugin:setting-soy",
                    templateName,
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
        
        pluginSettings.put(JENKINS_BASE_URL, req.getParameter("jenkinsBaseUrl"));
        pluginSettings.put(BUILD_REF_FIELD, req.getParameter("buildRefField"));
        
        resetSettings(pluginSettings);
        
        if (req.getParameter("triggerBuildOnCreate") != null) {
            pluginSettings.put(TRIGGER_BUILD_ON_CREATE, "checked");
        }
        
        if (req.getParameter("triggerBuildOnUpdate") != null) {
            pluginSettings.put(TRIGGER_BUILD_ON_UPDATE, "checked");
        }
        
        if (req.getParameter("triggerBuildOnReopen") != null) {
            pluginSettings.put(TRIGGER_BUILD_ON_REOPEN, "checked");
        }
        
        if (!req.getParameter("jenkinsUserName").isEmpty()) {
            pluginSettings.put(JENKINS_USERNAME, req.getParameter("jenkinsUserName")); 
        }
        if (!req.getParameter("jenkinsPassword").isEmpty()) {
            pluginSettings.put(JENKINS_PASSWORD, req.getParameter("jenkinsPassword"));
        }

        if (!req.getParameter("buildTitleField").isEmpty()) {
            pluginSettings.put(BUILD_TITLE_FIELD, req.getParameter("buildTitleField"));
        }
        
        resp.sendRedirect(req.getRequestURI());
    }

    private void resetSettings(PluginSettings pluginSettings) {
        pluginSettings.remove(JENKINS_USERNAME);
        pluginSettings.remove(JENKINS_PASSWORD);
        pluginSettings.remove(BUILD_TITLE_FIELD);
        
        pluginSettings.remove(TRIGGER_BUILD_ON_CREATE);
        pluginSettings.remove(TRIGGER_BUILD_ON_UPDATE);
        pluginSettings.remove(TRIGGER_BUILD_ON_REOPEN);
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

        if (components.length < 3) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Repository repository = repositoryService.getBySlug(components[1], components[2]);

        if (repository == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String template = "stash.config.jenkins.job.integration.repositorySettings";

        UserProfile user = userManager.getRemoteUser(req);
        if (user == null || (!userManager.isSystemAdmin(user.getUserKey()))) {
            redirectToLogin(req, resp);
        }
        
        Map<String, Object> context = Maps.newHashMap();

        resetFormFields(context);
        fillOutFormFields(repository, context);
        
        render(resp, template, context);
    }

    private void fillOutFormFields(Repository repository, Map<String, Object> context) {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        if (pluginSettings.get(JENKINS_BASE_URL) != null){
            context.put("jenkinsBaseUrl", pluginSettings.get(JENKINS_BASE_URL));
        }
        
        if (pluginSettings.get(JENKINS_USERNAME) != null){
            context.put("jenkinsUserName", pluginSettings.get(JENKINS_USERNAME));
        }
        
        if (pluginSettings.get(JENKINS_PASSWORD) != null){
            context.put("jenkinsPassword", pluginSettings.get(JENKINS_PASSWORD));
        }
        
        if (pluginSettings.get(BUILD_REF_FIELD) != null){
            context.put("buildRefField", pluginSettings.get(BUILD_REF_FIELD));
        }
        
        if (pluginSettings.get(BUILD_TITLE_FIELD) != null){
            context.put("buildTitleField", pluginSettings.get(BUILD_TITLE_FIELD));
        }
        
        if (pluginSettings.get(TRIGGER_BUILD_ON_CREATE) != null){
            context.put("triggerBuildOnCreate", "checked=\"checked\"");
        } 
        
        if (pluginSettings.get(TRIGGER_BUILD_ON_UPDATE) != null){
            context.put("triggerBuildOnUpdate", "checked=\"checked\"");
        } 
        
        if (pluginSettings.get(TRIGGER_BUILD_ON_REOPEN) != null){
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
