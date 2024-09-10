package com.shweit.itemlocator.commands;

import com.shweit.itemlocator.ItemLocator;

public abstract class RegisterCommands {
    public static void register() {
        CommandManager mainCommandManager = new CommandManager();
        ItemLocator.getInstance().getCommand("itemlocator").setExecutor(mainCommandManager);
    }
}
