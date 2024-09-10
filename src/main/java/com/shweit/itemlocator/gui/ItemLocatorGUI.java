package com.shweit.itemlocator.gui;

import com.shweit.itemlocator.ItemLocator;
import com.shweit.itemlocator.utils.DatabaseConnectionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class ItemLocatorGUI implements Listener {
    private final JavaPlugin plugin;
    private final List<Map<String, Object>> allItems;
    private int currentPage = 0;

    public ItemLocatorGUI(final Player player) {
        this.plugin = ItemLocator.getInstance();
        this.allItems = getItemsForPlayer(player.getUniqueId().toString());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public List<Map<String, Object>> getItemsForPlayer(final String playerUUID) {
        List<Map<String, Object>> itemsList = new ArrayList<>();

        // Query to group items by name, sum the amounts, and collect all coordinates for each item
        String query = "SELECT item, SUM(amount) AS total, GROUP_CONCAT(coordinates) AS coords "
                + "FROM items WHERE UUID = ? "
                + "GROUP BY item";

        try (Connection connection = new DatabaseConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, playerUUID); // Filter by player UUID
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String itemName = resultSet.getString("item");
                int totalQuantity = resultSet.getInt("total"); // Sum of all amounts for the item
                String coordinatesString = resultSet.getString("coords");

                // Parse the concatenated coordinates into a list
                List<String> coordinates = parseCoordinatesList(coordinatesString);

                // Create a map to represent each item
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("name", itemName);
                itemData.put("quantity", totalQuantity); // Use the summed total quantity
                itemData.put("coordinates", coordinates);

                // Add the item data to the list
                itemsList.add(itemData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return itemsList;
    }


    // Helper method to parse a concatenated string of coordinates into a list
    private List<String> parseCoordinatesList(final String coordinatesString) {
        List<String> coordinatesList = new ArrayList<>();
        if (coordinatesString != null && !coordinatesString.isEmpty()) {
            String[] coordsArray = coordinatesString.split(",");
            for (int i = 0; i < coordsArray.length; i += 3) {
                // Group every three elements (x, y, z) into a coordinate string
                String coords = "x:" + coordsArray[i] + ", y: " + coordsArray[i + 1] + ", z: " + coordsArray[i + 2];
                coordinatesList.add(coords);
            }
        }
        return coordinatesList;
    }

    // Method to create and open the inventory GUI
    public void openInventory(final org.bukkit.entity.Player player, final int page) {
        // Sort the allItems list by the quantity (amount) in descending order
        allItems.sort(Comparator.comparingInt(item -> -(int) item.get("quantity")));  // Sort by quantity, descending

        // Create inventory with 54 slots (45 for items, 9 for control buttons)
        Inventory inventory = Bukkit.createInventory(null, 54, "Item Locator - Page " + (page + 1));

        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());

        // Fill the inventory with items for the current page
        for (int i = startIndex; i < endIndex; i++) {
            Map<String, Object> itemData = allItems.get(i); // Assumes `allItems` contains the data from getAllItemsForPlayer
            ItemStack item = createItemStack(itemData); // Create the item stack with the sorted data
            inventory.setItem(i - startIndex, item);
        }

        // Fill the bottom row with gray stained glass panes to indicate it's not usable
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, createGrayPane());
        }

        // Add navigation buttons
        if (page > 0) {
            // Add "Previous Page" button (left arrow)
            inventory.setItem(45, createNavigationItem(Material.ARROW, "Previous Page"));
        }
        if (endIndex < allItems.size()) {
            // Add "Next Page" button (right arrow)
            inventory.setItem(53, createNavigationItem(Material.ARROW, "Next Page"));
        }

        // Open the inventory for the player
        player.openInventory(inventory);
    }


    // Helper method to create item stacks with rounded quantity and coordinates in lore
    private ItemStack createItemStack(final Map<String, Object> itemData) {
        String itemName = (String) itemData.get("name");
        int originalQuantity = (int) itemData.get("quantity");
        List<String> coordinates = (List<String>) itemData.get("coordinates");

        // Create the item stack for the specific item
        ItemStack itemStack = new ItemStack(Material.valueOf(itemName), originalQuantity); // Set the appropriate stack size
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            // Set the lore (original quantity and coordinates) with better formatting
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Total: " + ChatColor.GREEN + originalQuantity);

            // Add a blank line for better formatting
            lore.add("");

            lore.add(ChatColor.AQUA + "Coordinates:");
            for (String coord : coordinates) {
                lore.add("  " + ChatColor.GRAY + coord); // Add each coordinate in gray color
            }

            // Set the lore to the meta
            meta.setLore(lore);

            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    // Helper method to create gray stained-glass panes
    private ItemStack createGrayPane() {
        ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta meta = grayPane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" "); // Set the display name to an empty string
            grayPane.setItemMeta(meta);
        }
        return grayPane;
    }

    // Helper method to create navigation items
    private ItemStack createNavigationItem(final Material material, final String name) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Event handler for when a player clicks in the inventory
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Item Locator")) {
            event.setCancelled(true);  // Prevent players from taking items from the GUI

            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
                return;
            }

            String itemName = event.getCurrentItem().getItemMeta().getDisplayName();

            if (itemName.equals("Next Page")) {
                openInventory((org.bukkit.entity.Player) event.getWhoClicked(), currentPage + 1);
                currentPage++;
            } else if (itemName.equals("Previous Page")) {
                openInventory((org.bukkit.entity.Player) event.getWhoClicked(), currentPage - 1);
                currentPage--;
            }
        }
    }
}
