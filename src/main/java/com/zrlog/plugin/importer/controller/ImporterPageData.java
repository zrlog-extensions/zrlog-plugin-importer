package com.zrlog.plugin.importer.controller;

import com.zrlog.plugin.message.Plugin;

public class ImporterPageData {

    private boolean dark;
    private String colorPrimary;
    private Plugin plugin;

    public boolean isDark() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
    }

    public String getColorPrimary() {
        return colorPrimary;
    }

    public void setColorPrimary(String colorPrimary) {
        this.colorPrimary = colorPrimary;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }
}
