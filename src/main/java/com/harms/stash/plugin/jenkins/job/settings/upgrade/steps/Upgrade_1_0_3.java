package com.harms.stash.plugin.jenkins.job.settings.upgrade.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.Upgrade;

/**
 * Upgrading step for jenkins CI server list for support for load balancing jobs
 * @author fharms
 *
 */
public class Upgrade_1_0_3 implements Upgrade {

    private static final Logger log = LoggerFactory.getLogger(Upgrade_1_0_3.class);
    private final String slug;

    public Upgrade_1_0_3(String slug) {
        this.slug = slug;
    }
    @Override
    public void perform(PluginSettings ps) {
        String jenkinBaseUrl = (String) ps.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_BASE_URL,slug));
        if (jenkinBaseUrl != null) {
            log.info("Adding jenkins base url to the CI server list");;
            ps.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_CI_SERVER_LIST,slug),jenkinBaseUrl);
        }
    }

    @Override
    public String getVersion() {
        return "1.0.3";
    }

}
