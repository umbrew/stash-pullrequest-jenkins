package com.harms.stash.plugin.jenkins.job.settings.upgrade;

import java.util.ArrayList;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

/**
 * Process all upgrade step from one version to another version.
 * 
 * @author fharms
 *
 */
public class UpgradeService  {

    private ArrayList<Upgrade> upgradeStep = new ArrayList<Upgrade>();
    private PluginSettings ps;
    
    /**
     * @param psf - the {@link PluginSettingsFactory} used for accessing the plugin settings
     */
    public UpgradeService(PluginSettingsFactory psf) {
        this.ps = psf.createGlobalSettings();
    }
    
    /**
     * Process the list of upgrade scripts by running through the list
     * The only requirement is the upgrade has not been executed before, this
     * match against the current plug-in version and the upgrade version.
     * <pre>
     * The order of the steps is decided by the order it's added with the
     * <code>addUpgradeStep(Upgrade upgrade)</code>
     * </pre> 
     */
    public void process() {
        for (Upgrade upgradeStep : this.upgradeStep) {
            if (upgradeStep.getVersion().compareTo(getCurrentVersion()) > 0) {
                upgradeStep.perform(ps);
                setCurrentVersion(upgradeStep.getVersion());
            }
        }
    }
    
    /**
     * Add a upgrade step to the upgrade process
     * @param upgrade
     */
    public void addUpgradeStep(Upgrade upgrade) {
        upgradeStep.add(upgrade);
    }
    
    /**
     * @return the current version of the plug-in
     */
    private String getCurrentVersion() {
        String version = (String)ps.get(PluginSettingsHelper.PLUGIN_VERISON);
        return version == null? "0":version;
    }
    
    /**
     * Set the plug-in version
     * @param version
     */
    private void setCurrentVersion(String version) {
        ps.put(PluginSettingsHelper.PLUGIN_VERISON,version);
    }
}
