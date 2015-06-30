package com.harms.stash.plugin.jenkins.job.settings.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.google.common.collect.Maps;
import com.harms.stash.plugin.jenkins.job.settings.DecryptException;
import com.harms.stash.plugin.jenkins.job.settings.EncryptException;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.UpgradeService;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_1;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_2;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_3;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_5;

public class JenkinsIntegrationPluginSettingsServlet extends JenkinsStashBaseServlet {
    private static final long serialVersionUID = -1645440333554544743L;

    private static final Logger log = LoggerFactory.getLogger(JenkinsIntegrationPluginSettingsServlet.class);
    private final SoyTemplateRenderer soyTemplateRenderer;
    private final RepositoryService repositoryService;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final UpgradeService upgradeService;

    public JenkinsIntegrationPluginSettingsServlet(SoyTemplateRenderer soyTemplateRenderer, RepositoryService repositoryService, PluginSettingsFactory pluginSettingsFactory, StashAuthenticationContext stashAuthContext, LoginUriProvider loginUriProvider) {
        super(loginUriProvider, stashAuthContext);
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.repositoryService = repositoryService;
        this.pluginSettingsFactory = pluginSettingsFactory;
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
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String pathInfo = req.getPathInfo();

        String[] components = pathInfo.split("/");

        if (components.length < 3) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (req.getParameter("disableBuildParameter") != null) {
            Repository repository = repositoryService.getBySlug(components[1], components[2]);
            updatePullRequestSetttings(pluginSettings, req.getParameter("disableBuildParameter"), repository.getProject().getKey(),repository.getSlug(),Long.valueOf(components[3]));
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
        PluginSettingsHelper.resetSettings(slug,ps);

        String[] jenkinsCIServerList = parameterMap.get("jenkinsCIServerList");
        if (jenkinsCIServerList != null) {
            PluginSettingsHelper.setJenkinsCIServerList(jenkinsCIServerList, slug, ps);
        }

        PluginSettingsHelper.setBuildReferenceField(slug, parameterMap.get("buildRefField")[0], ps);
        PluginSettingsHelper.setBuildDelay(slug, new Integer(parameterMap.get("buildDelayField")[0]), ps);

        if (parameterMap.containsKey("disableAutomaticBuildByDefault")) {
            PluginSettingsHelper.enableDisableAutomaticBuildByDefault(slug, ps);
        }

        if (parameterMap.containsKey("triggerBuildOnCreate")) {
            PluginSettingsHelper.enableTriggerOnCreate(slug, ps);
        }

        if (parameterMap.containsKey("triggerBuildOnUpdate")) {
            PluginSettingsHelper.enableTriggerOnUpdate(slug, ps);
        }

        if (parameterMap.containsKey("triggerBuildOnReopen")) {
            PluginSettingsHelper.enableTriggerOnReopen(slug, ps);
        }

        if (!parameterMap.get("jenkinsUserName")[0].isEmpty()) {
            PluginSettingsHelper.setUsername(slug, parameterMap.get("jenkinsUserName")[0].getBytes(), ps);
        }
        if (!parameterMap.get("jenkinsPassword")[0].isEmpty()) {
            PluginSettingsHelper.setPassword(slug, parameterMap.get("jenkinsPassword")[0].getBytes(), ps);
        }

        if (!parameterMap.get("buildTitleField")[0].isEmpty()) {
            PluginSettingsHelper.setBuildTitleField(slug, parameterMap.get("buildTitleField")[0], ps);
        }

        if (!parameterMap.get("fromBranchField")[0].isEmpty()) {
            PluginSettingsHelper.setFromBranchField(slug, parameterMap.get("fromBranchField")[0], ps);
        }

        if (!parameterMap.get("toBranchField")[0].isEmpty()) {
            PluginSettingsHelper.setToBranchField(slug, parameterMap.get("toBranchField")[0], ps);
        }

        if (!parameterMap.get("buildPullRequestUrlField")[0].isEmpty()) {
            PluginSettingsHelper.setPullRequestUrlFieldName(slug, parameterMap.get("buildPullRequestUrlField")[0], ps);
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
    private void updatePullRequestSetttings(PluginSettings pluginSettings, String disableAutomaticBuildParameter,String projectKey, String slug, Long pullRequestId) {
        if (disableAutomaticBuildParameter.isEmpty()) {
            PluginSettingsHelper.clearAutomaticBuildFlag(projectKey,slug,pullRequestId,pluginSettings);
        } else {
            PluginSettingsHelper.enableAutomaticBuildFlag(projectKey,slug,pullRequestId,pluginSettings);
        }
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

        if (components.length == 4) {
            //handle settings for a pull-request
            String readPullRequestSettings = readPullRequestSettings(resp, new Long(components[3]), repository);
            if (readPullRequestSettings != null) {
                resp.setContentType("application/json");
                resp.getWriter().print("{\"form\":\""+readPullRequestSettings+"\"}");
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
        upgradeService.addUpgradeStep(new Upgrade_1_0_5(repository.getSlug()));
        upgradeService.process(); //if the upgrade is already executed this will just return
    }

    private String readPullRequestSettings(HttpServletResponse resp, Long pullRequestId, Repository repository) throws IOException {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        return PluginSettingsHelper.isAutomaticBuildDisabled(repository.getProject().getKey(), repository.getSlug(), pullRequestId, pluginSettings) ? "CHECKED":"";
    }

    private void readPluginSettings(Repository repository, Map<String, Object> context) {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String slug = repository.getSlug();

        context.put("jenkinsCIServerList", PluginSettingsHelper.getJenkinsCIServerList(slug, pluginSettings));

        try {
            context.put("jenkinsUserName",new String(PluginSettingsHelper.getUsername(slug, pluginSettings)));
        } catch (DecryptException e) {
            log.error("Not able to decrypt the username, will reset the field. You will have to re-enter username",e);
            context.put("jenkinsUserName","");
        }

        try {
            context.put("jenkinsPassword", new String(PluginSettingsHelper.getPassword(slug, pluginSettings)));
        } catch (DecryptException e) {
            log.error("Not able to decrypt the password, will reset the field. You will have to re-enter password",e);
            context.put("jenkinsPassword","");
        }

        context.put("buildRefField", PluginSettingsHelper.getBuildReferenceField(slug, pluginSettings));
        context.put("buildDelayField", PluginSettingsHelper.getBuildDelay(slug, pluginSettings).toString());
        context.put("buildTitleField", PluginSettingsHelper.getBuildTitleField(slug, pluginSettings));
        context.put("fromBranchField", PluginSettingsHelper.getFromBranchField(slug, pluginSettings));
        context.put("toBranchField", PluginSettingsHelper.getToBranchField(slug, pluginSettings));
        context.put("buildPullRequestUrlField", PluginSettingsHelper.getPullRequestUrlFieldName(slug, pluginSettings));

        if (PluginSettingsHelper.isDisableAutomaticBuildByDefault(slug, pluginSettings)){
            context.put("disableAutomaticBuildByDefault", "checked=\"checked\"");
        }

        if (PluginSettingsHelper.isTriggerBuildOnCreate(slug, pluginSettings)){
            context.put("triggerBuildOnCreate", "checked=\"checked\"");
        }

        if (PluginSettingsHelper.isTriggerBuildOnUpdate(slug, pluginSettings)){
            context.put("triggerBuildOnUpdate", "checked=\"checked\"");
        }

        if (PluginSettingsHelper.isTriggerBuildOnReopen(slug, pluginSettings)){
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
        context.put("disableAutomaticBuildByDefault", "");
        context.put("triggerBuildOnCreate", "");
        context.put("triggerBuildOnUpdate", "");
        context.put("triggerBuildOnReopen", "");
        context.put("buildPullRequestUrlField", "");
        context.put("buildDelayField", "");
    }

}
