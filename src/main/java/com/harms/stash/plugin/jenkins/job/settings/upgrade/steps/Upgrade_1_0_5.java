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
public class Upgrade_1_0_5 implements Upgrade {

    private static final Logger log = LoggerFactory.getLogger(Upgrade_1_0_5.class);
    private final String slug;

    public Upgrade_1_0_5(String slug) {
        this.slug = slug;
    }
    @Override
    public void perform(PluginSettings ps) {
        PluginSettingsHelper.setBuildDelay(slug, 0, ps);
        log.info("Upgrade build delay field for supporting delayed build");
    }

    @Override
    public String getVersion() {
        return "1.0.5";
    }

}
