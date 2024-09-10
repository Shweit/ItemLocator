package com.shweit.itemlocator.commands;

import com.shweit.itemlocator.ItemLocator;
import com.shweit.itemlocator.gui.ItemLocatorGUI;
import com.shweit.itemlocator.utils.DatabaseConnectionManager;
import com.shweit.itemlocator.utils.Translator;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
                    player.sendMessage(ChatColor.RED + Translator.getTranslation("no_item_found", Map.of("item", material.name())));
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

                spawnGlowingMarker(player, coordinates);
                spawnFakeBeaconBeam(player, coordinates);
            } else {
                player.sendMessage(ChatColor.RED + Translator.getTranslation("no_item_found", Map.of("item", material.name())));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "An error occurred while trying to retrieve the data.");
        }
    }

    public void spawnGlowingMarker(final Player player, final List<String> coordinatesList) {
        for (String coord : coordinatesList) {
            String[] parts = coord.replace("x:", "").replace("y:", "").replace("z:", "").split(",");
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());

            // Adjust the height so the Armor Stand is floating above the chest
            Location location = new Location(player.getWorld(), x + 0.5, y + 1.5, z + 0.5);

            // Spawn the Armor Stand and cast it correctly
            ArmorStand marker = location.getWorld().spawn(location, ArmorStand.class);

            // Now we can apply the specific Armor Stand properties
            marker.setVisible(false);   // Make it invisible
            marker.setMarker(true);     // Make it a marker (no hitbox)
            marker.setInvulnerable(true); // Make it invulnerable
            marker.setCustomName("§aItem Location"); // Optional: Give it a custom name
            marker.setCustomNameVisible(true); // Optional: Show the name
            marker.setGlowing(true); // Make it glow

            // Schedule a task to remove the marker after 10 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    marker.remove(); // Remove the marker after 10 seconds
                }
            }.runTaskLater(ItemLocator.getInstance(), 200L); // 200 ticks = 10 seconds
        }
    }

    public void spawnFakeBeaconBeam(final Player player, final List<String> coordinatesList) {
        for (String coord : coordinatesList) {
            String[] parts = coord.replace("x:", "").replace("y:", "").replace("z:", "").split(",");
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());

            Location location = new Location(player.getWorld(), x + 0.5, y, z + 0.5);
            double maxHeight = player.getWorld().getMaxHeight();  // Max build height
            double startY = location.getY();

            // Create the particle beam from startY to maxHeight in one tick
            new BukkitRunnable() {
                int tickCount = 0;  // Track how many ticks have passed

                @Override
                public void run() {
                    // Stop after 20 seconds (400 ticks)
                    if (tickCount >= 200) {
                        this.cancel();
                        return;
                    }

                    // Create the entire beam from the current block to the max height
                    for (double y = startY; y < maxHeight; y += 0.5) {
                        Location particleLocation = new Location(location.getWorld(), location.getX(), y, location.getZ());

                        // Show a particle (this is the fake beam)
                        player.getWorld().spawnParticle(Particle.END_ROD, particleLocation, 0, 0, 1, 0, 0.1);
                    }

                    // Increment the tick counter
                    tickCount++;
                }
            }.runTaskTimer(ItemLocator.getInstance(), 0L, 1L);  // Run every tick for 20 seconds
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
