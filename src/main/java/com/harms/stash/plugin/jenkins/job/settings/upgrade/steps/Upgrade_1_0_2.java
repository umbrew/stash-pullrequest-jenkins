package com.harms.stash.plugin.jenkins.job.settings.upgrade.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.harms.stash.plugin.jenkins.job.settings.EncryptException;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.Upgrade;

/**
 * Upgrading step for encrypt password and username
 * @author fharms
 *
 */
public class Upgrade_1_0_2 implements Upgrade {

    private static final Logger log = LoggerFactory.getLogger(Upgrade_1_0_2.class);
    private final String slug;

    public Upgrade_1_0_2(String slug) {
        this.slug = slug;
    }
    @Override
    public void perform(PluginSettings ps) {
        String userName = (String) ps.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,slug));
        String password = (String) ps.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,slug));
        try {
            if (userName != null && password != null) {
                PluginSettingsHelper.setUsername(slug, userName.getBytes(), ps);
                PluginSettingsHelper.setPassword(slug, password.getBytes(), ps);
            }
        } catch (EncryptException e) {
          log.error("Not able to upgrade from 1.0.1 to 1.0.2 because of problem in encrypting username and password", e);
        }
    }

    @Override
    public String getVersion() {
        return "1.0.2";
    }

}
