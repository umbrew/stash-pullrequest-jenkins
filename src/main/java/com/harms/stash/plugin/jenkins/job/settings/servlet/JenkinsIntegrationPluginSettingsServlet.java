package com.harms.stash.plugin.jenkins.job.settings.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.google.common.collect.Maps;
import com.harms.stash.plugin.jenkins.job.settings.DecryptException;
import com.harms.stash.plugin.jenkins.job.settings.EncryptException;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.UpgradeService;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_1;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_2;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_3;

public class JenkinsIntegrationPluginSettingsServlet extends HttpServlet {
    private static final long serialVersionUID = -1645440333554544743L;
    
    private static final Logger log = LoggerFactory.getLogger(JenkinsIntegrationPluginSettingsServlet.class);
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
           try {
               updatePluginSettings(pluginSettings, req.getParameterMap());
            } catch (EncryptException e) {
                throw new ServletException("Not able to update the settings",e);
            }
           resp.sendRedirect(req.getRequestURI());
        }    
    }

   
    /**
     * Update plugin settings from the parameterMap
     * @param ps - {@link PluginSettings} 
     * @param parameterMap - A map containing parameter name and value 
     * @throws EncryptException 
     */
    private void updatePluginSettings(PluginSettings ps, Map<String, String[]> parameterMap) throws EncryptException {
        String slug = parameterMap.get("repository.slug")[0];
        resetSettings(ps,slug);
        
        String[] jenkinsCIServerList = parameterMap.get("jenkinsCIServerList");
        if (jenkinsCIServerList != null) {
            String concatString = Arrays.toString(jenkinsCIServerList).trim();
            ps.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_CI_SERVER_LIST,slug), concatString.substring(1, concatString.length()-1));
        }
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
            PluginSettingsHelper.setUsername(slug, parameterMap.get("jenkinsUserName")[0].getBytes(), ps);
        }
        if (!parameterMap.get("jenkinsPassword")[0].isEmpty()) {
            PluginSettingsHelper.setPassword(slug, parameterMap.get("jenkinsPassword")[0].getBytes(), ps);
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
        upgradeService.addUpgradeStep(new Upgrade_1_0_2(repository.getSlug()));
        upgradeService.addUpgradeStep(new Upgrade_1_0_3(repository.getSlug()));
        upgradeService.process(); //if the upgrade is already executed this will just return
    }

    private String readPullRequestSettings(HttpServletResponse resp, String pullRequestId, Repository repository) throws IOException {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String key = "stash.plugin.jenkins.settingsui." + repository.getProject().getKey() + "/" + repository.getSlug()+ "/" +pullRequestId;
        return (String) pluginSettings.get(key);
    }

    private void readPluginSettings(Repository repository, Map<String, Object> context) {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String slug = repository.getSlug();
        
        String ciServerList = PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_CI_SERVER_LIST,slug);
        if (pluginSettings.get(ciServerList) != null){
            String[] serverList = ((String)pluginSettings.get(ciServerList)).split(",");
            context.put("jenkinsCIServerList", serverList);
        }
        
        String usernameKey = PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,slug);
        if (pluginSettings.get(usernameKey) != null){
            try {
                context.put("jenkinsUserName",new String(PluginSettingsHelper.getUsername(slug, pluginSettings)));
            } catch (DecryptException e) {
                log.error("Not able to decrypt the username, will reset the field. You will have to re-enter username",e);
                context.put("jenkinsUserName","");
            }
        }
        
        String passwordKey = PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,slug);
        if (pluginSettings.get(passwordKey) != null){
            try {
                context.put("jenkinsPassword", new String(PluginSettingsHelper.getPassword(slug, pluginSettings)));
            } catch (DecryptException e) {
                log.error("Not able to decrypt the password, will reset the field. You will have to re-enter password",e);
                context.put("jenkinsPassword","");
            }
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
        context.put("jenkinsCIServerList",null);
        context.put("triggerBuildOnCreate", "");
        context.put("triggerBuildOnUpdate", "");
        context.put("triggerBuildOnReopen", "");
    }

}
