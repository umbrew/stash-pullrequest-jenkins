package com.harms.stash.plugin.jenkins.job.settings.upgrade.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.Upgrade;

/**
 * Upgrading step for support of delayed builds
 * @author fharms
 *
 */
public class Upgrade_1_0_6 implements Upgrade {

    private static final Logger log = LoggerFactory.getLogger(Upgrade_1_0_6.class);
    private final String slug;

    public Upgrade_1_0_6(String slug) {
        this.slug = slug;
    }
    @Override
    public void perform(PluginSettings ps) {
        PluginSettingsHelper.setFromBranchField(slug, PluginSettingsHelper.getFromBranchField(slug, ps), ps);
        PluginSettingsHelper.setToBranchField(slug, PluginSettingsHelper.getToBranchField(slug, ps), ps);
        log.info("Upgrade build from and to branch field for supporting branch info to jenkins");
    }

    @Override
    public String getVersion() {
        return "1.0.5";
    }

}
