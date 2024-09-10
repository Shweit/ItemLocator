package com.shweit.itemlocator.commands;

import com.shweit.itemlocator.gui.ItemLocatorGUI;
import com.shweit.itemlocator.utils.DatabaseConnectionManager;
import com.shweit.itemlocator.utils.Translator;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CommandManager implements TabExecutor {

    public CommandManager() { }

    @Override
    public boolean onCommand(final CommandSender commandSender, final Command command, final String label, final String[] args) {
        if (args.length > 0) {
            // Check if the first argument is a valid material
            Material material = Material.matchMaterial(args[0]);
            if (material != null) {
                if (commandSender instanceof Player player) {
                    locateItem(player, material);
                } else {
                    commandSender.sendMessage(ChatColor.RED + Translator.getTranslation("player_only_command"));
                }
            } else {
                commandSender.sendMessage(ChatColor.RED + Translator.getTranslation("invalid_material"));
            }
        } else {
            if (commandSender instanceof Player player) {
                new ItemLocatorGUI(player).openInventory(player, 0);
            } else {
                commandSender.sendMessage(ChatColor.RED + Translator.getTranslation("player_only_command"));
            }
        }

        return true;
    }

    private void locateItem(final Player player, final Material material) {
        String query = "SELECT item, SUM(amount) AS total, GROUP_CONCAT(coordinates) AS coords "
                + "FROM items WHERE UUID = ? "
                + "AND item = ? ";

        try (Connection connection = new DatabaseConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, player.getUniqueId().toString()); // Filter by player UUID
            statement.setString(2, material.name());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) { // Use if instead of while, since you're expecting one result
                String item = resultSet.getString("item");
                String total = resultSet.getString("total");
                String coordinatesString = resultSet.getString("coords");

                // Handle potential null values for "item" and "total"
                if (item == null || total == null || coordinatesString == null) {
                    player.sendMessage(ChatColor.RED + "No data found for the requested item.");
                    return;
                }

                // Parse the coordinates
                List<String> coordinates = parseCoordinatesList(coordinatesString);

                player.sendMessage(ChatColor.GREEN + Translator.getTranslation("found_item", Map.of(
                        "item", item,
                        "total", total
                )));

                for (String coord : coordinates) {
                    player.sendMessage("  " + ChatColor.GRAY + coord);
                }
            } else {
                player.sendMessage(ChatColor.RED + "No entries found for the item: " + material.name());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "An error occurred while trying to retrieve the data.");
        }
    }


    // Helper method to parse a concatenated string of coordinates into a list
    private List<String> parseCoordinatesList(final String coordinatesString) {
        List<String> coordinatesList = new ArrayList<>();
        if (coordinatesString != null && !coordinatesString.isEmpty()) {
            String[] coordsArray = coordinatesString.split(",");
            for (int i = 0; i < coordsArray.length; i += 3) {
                // Group every three elements (x, y, z) into a coordinate string
                String coords = "x: " + coordsArray[i] + ", y: " + coordsArray[i + 1] + ", z: " + coordsArray[i + 2];
                coordinatesList.add(coords);
            }
        }
        return coordinatesList;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            List<String> materials = new ArrayList<>();
            for (Material material : Material.values()) {
                materials.add(material.name().toLowerCase());
            }
            return materials;
        }

        return null;
    }
}
