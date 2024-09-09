package com.shweit.itemlocator.listeners;

import com.shweit.itemlocator.utils.DatabaseConnectionManager;
import com.shweit.itemlocator.utils.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class InventoryClose implements Listener {

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (isValidContainer(inventory)) {
            String playerUUID = event.getPlayer().getUniqueId().toString();
            String coordinates = event.getInventory().getLocation().getBlockX() + ","
                    + event.getInventory().getLocation().getBlockY() + ","
                    + event.getInventory().getLocation().getBlockZ();

            // Map to store total amount of each item type
            Map<String, Integer> itemCounts = new HashMap<>();

            for (ItemStack item : inventory.getContents()) {
                if (item != null) {
                    String itemName = item.getType().name();
                    int amount = item.getAmount();

                    // Add item count to the map (if exists, sum up the amount)
                    itemCounts.put(itemName, itemCounts.getOrDefault(itemName, 0) + amount);
                }
            }

            // Now save each item and its total amount to the database
            for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                String itemName = entry.getKey();
                int totalAmount = entry.getValue();

                // Save the summed item stack to the database
                saveItemToDatabase(playerUUID, coordinates, inventory.getType().name(), itemName, totalAmount);
            }
        }
    }

    private boolean isValidContainer(final Inventory inventory) {
        return switch (inventory.getType()) {
            case CHEST, DISPENSER, DROPPER, FURNACE, HOPPER, SHULKER_BOX, BARREL, BLAST_FURNACE, SMOKER -> true;
            default -> false;
        };
    }

    private void saveItemToDatabase(final String playerUUID, final String coordinates, final String container, final String itemName, final int amount) {
        String query = "INSERT OR REPLACE INTO items (item, amount, coordinates, container, UUID) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = new DatabaseConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, itemName);
            statement.setInt(2, amount);
            statement.setString(3, coordinates);
            statement.setString(4, container);
            statement.setString(5, playerUUID);
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Failed to save item to the database");
            e.printStackTrace();
        }
    }
}
