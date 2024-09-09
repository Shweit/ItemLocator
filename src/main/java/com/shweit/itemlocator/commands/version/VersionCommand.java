package com.shweit.itemlocator.commands.version;

import com.shweit.itemlocator.ItemLocator;
import com.shweit.itemlocator.commands.SubCommand;
import com.shweit.itemlocator.utils.Translator;
import com.shweit.itemlocator.utils.UpdateChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VersionCommand extends SubCommand {
    @Override
    public String getName() {
        return "version";
    }

    @Override
    public String getDescription() {
        return "Shows the current version of the plugin.";
    }

    @Override
    public String getSyntax() {
        return "/mcapi version";
    }

    @Override
    public void perform(final CommandSender commandSender, final Command command, final String label, final String[] args) {
        Map<String, String> params = new HashMap<>();
        params.put("version", ItemLocator.getInstance().getDescription().getVersion());
        commandSender.sendMessage(ChatColor.GREEN + Translator.getTranslation("current_version", params));
        commandSender.sendMessage("");
        commandSender.sendMessage(ChatColor.GREEN + Translator.getTranslation("checking_for_updates"));

        // Check for updates
        UpdateChecker checkForUpdate = new UpdateChecker();
        boolean updateAvailable = checkForUpdate.checkForPluginUpdate();
        if (updateAvailable) {
            commandSender.sendMessage(ChatColor.GREEN + Translator.getTranslation("update_available", Map.of("version", checkForUpdate.latestVersion)));
        } else {
            commandSender.sendMessage(ChatColor.GREEN + Translator.getTranslation("no_update_available"));
        }
    }

    @Override
    public List<String> getSubcommandArguments(final CommandSender commandSender, final Command command, final String label, final String[] args) {
        return List.of();
    }
}
