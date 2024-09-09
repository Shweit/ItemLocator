package com.shweit.untitled;

import com.shweit.untitled.commands.RegisterCommands;
import com.shweit.untitled.utils.Translator;
import com.shweit.untitled.utils.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public final class Untitled extends JavaPlugin {

    public static FileConfiguration config;

    @Override
    public void onEnable() {
        createConfig();
        config = getConfig();
        Translator.loadLanguageFile();

        getServer().getPluginManager().registerEvents(new UpdateChecker(), this);
        RegisterCommands.register();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void createConfig() {
        saveDefaultConfig();

        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        saveResource("lang/en.yml", false);
    }

    public static Untitled getInstance() {
        return getPlugin(Untitled.class);
    }
}
