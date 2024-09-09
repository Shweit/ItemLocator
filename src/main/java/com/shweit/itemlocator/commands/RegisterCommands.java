package com.shweit.itemlocator.commands;

import com.shweit.itemlocator.ItemLocator;
import com.shweit.itemlocator.commands.version.VersionCommand;

import java.util.List;

public abstract class RegisterCommands {
    public static void register() {
        List<SubCommand> subCommands = List.of(
            new VersionCommand()
        );
        CommandManager mainCommandManager = new CommandManager(subCommands);
        ItemLocator.getInstance().getCommand("itemlocator").setExecutor(mainCommandManager);
    }
}
