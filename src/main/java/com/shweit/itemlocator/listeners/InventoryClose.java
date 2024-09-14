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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class InventoryClose implements Listener {

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (isValidContainer(event)) {
            String playerUUID = event.getPlayer().getUniqueId().toString();
            String coordinates = event.getInventory().getLocation().getBlockX() + ","
                    + event.getInventory().getLocation().getBlockY() + ","
                    + event.getInventory().getLocation().getBlockZ();

            // Map to store total amount of each item type in the current inventory
            Map<String, Integer> itemCounts = new HashMap<>();

            for (ItemStack item : inventory.getContents()) {
                if (item != null) {
                    String itemName = item.getType().name();
                    int amount = item.getAmount();

                    // Add item count to the map (if exists, sum up the amount)
                    itemCounts.put(itemName, itemCounts.getOrDefault(itemName, 0) + amount);
                }
            }

            // Load current items from the database for this container
            Set<String> existingItems = loadItemsFromDatabase(playerUUID, coordinates);

            // Now save each item and its total amount to the database
            for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                String itemName = entry.getKey();
                int totalAmount = entry.getValue();

                // Mark the item as processed (it exists in the current inventory)
                existingItems.remove(itemName);

                // Save the summed item stack to the database
                saveItemToDatabase(playerUUID, coordinates, inventory.getType().name(), itemName, totalAmount);
            }

            // Remove items from the database that are no longer in the container
            for (String itemName : existingItems) {
                deleteItemFromDatabase(playerUUID, coordinates, itemName);
            }
        }
    }

    private boolean isValidContainer(final InventoryCloseEvent event) {
        boolean validContainer;
        validContainer = switch (event.getInventory().getType()) {
            case CHEST, DISPENSER, DROPPER, FURNACE, HOPPER, SHULKER_BOX, BARREL, BLAST_FURNACE, SMOKER -> true;
            default -> false;
        };

        if (event.getView().getTitle().startsWith("Item Locator")) {
            validContainer = false;
        }

        return validContainer;
    }

    private Set<String> loadItemsFromDatabase(final String playerUUID, final String coordinates) {
        Set<String> items = new HashSet<>();
        String query = "SELECT item FROM items WHERE UUID = ? AND coordinates = ?";

        try (Connection connection = new DatabaseConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, playerUUID);
            statement.setString(2, coordinates);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                items.add(resultSet.getString("item"));
            }
        } catch (SQLException e) {
            Logger.error("Failed to load items from the database");
            e.printStackTrace();
        }

        return items;
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

    private void deleteItemFromDatabase(final String playerUUID, final String coordinates, final String itemName) {
        String query = "DELETE FROM items WHERE UUID = ? AND coordinates = ? AND item = ?";

        try (Connection connection = new DatabaseConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID);
            statement.setString(2, coordinates);
            statement.setString(3, itemName);
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Failed to delete item from the database");
            e.printStackTrace();
        }
    }
}
