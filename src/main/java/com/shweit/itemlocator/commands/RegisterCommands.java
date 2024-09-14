package com.shweit.itemlocator.commands;

import com.shweit.itemlocator.ItemLocator;

import java.util.List;

public abstract class RegisterCommands {
    public static void register() {
        CommandManager mainCommandManager = new CommandManager(List.of(
            new LocateItemCommand(),
            new LocateItemGuiCommand()
        ));
        ItemLocator.getInstance().getCommand("itemlocator").setExecutor(mainCommandManager);
    }
}
