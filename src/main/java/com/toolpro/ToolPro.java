package com.toolpro;

import com.toolpro.modules.AutoDTap;
import com.toolpro.modules.BetterAutoTotem;
import com.toolpro.modules.PearlAnchor;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class ToolPro extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    /**
     * Category that groups all modules added by this addon inside the Meteor Client GUI.
     */
    public static final Category CATEGORY = new Category("ToolPRO");

    @Override
    public void onInitialize() {
        LOG.info("Initializing ToolPRO addon");

        Modules modules = Modules.get();
        modules.add(new AutoDTap());
        modules.add(new PearlAnchor());
        modules.add(new BetterAutoTotem());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.toolpro";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("sam65578578578", "ToolPRO");
    }
}
