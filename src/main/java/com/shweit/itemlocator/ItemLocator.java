package com.shweit.itemlocator;

import com.shweit.itemlocator.commands.RegisterCommands;
import com.shweit.itemlocator.listeners.InventoryClose;
import com.shweit.itemlocator.listeners.ShulkerBreakListener;
import com.shweit.itemlocator.utils.DatabaseConnectionManager;
import com.shweit.itemlocator.utils.Translator;
import com.shweit.itemlocator.utils.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public final class ItemLocator extends JavaPlugin {

    public static FileConfiguration config;

    @Override
    public void onEnable() {
        createConfig();
        config = getConfig();
        Translator.loadLanguageFile();
        new DatabaseConnectionManager().setUpDatabase();
        registerListeners();
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

    public static ItemLocator getInstance() {
        return getPlugin(ItemLocator.class);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new UpdateChecker(), this);
        getServer().getPluginManager().registerEvents(new InventoryClose(), this);
        getServer().getPluginManager().registerEvents(new ShulkerBreakListener(), this);
    }
}
