package com.shweit.itemlocator.commands;

import com.shweit.itemlocator.gui.ItemLocatorGUI;
import com.shweit.itemlocator.utils.Translator;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class LocateItemGuiCommand extends SubCommand {
    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public String getDescription() {
        return "Opens the GUI to select that is being searched.";
    }

    @Override
    public String getSyntax() {
        return "/itemlocator gui";
    }

    @Override
    public void perform(final CommandSender commandSender, final Command command, final String label, final String[] args) {
        if (commandSender instanceof Player player) {
            new ItemLocatorGUI(player).openInventory(player, 0);
        } else {
            commandSender.sendMessage(ChatColor.RED + Translator.getTranslation("player_only_command"));
        }
    }

    @Override
    public List<String> getSubcommandArguments(final CommandSender commandSender, final Command command, final String label, final String[] args) {
        return List.of();
    }
}
