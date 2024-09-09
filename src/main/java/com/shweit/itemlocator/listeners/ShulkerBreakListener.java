package com.shweit.itemlocator.listeners;

import com.shweit.itemlocator.utils.DatabaseConnectionManager;
import com.shweit.itemlocator.utils.Logger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class ShulkerBreakListener implements Listener {

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        Block block = event.getBlock();

        // Check if the block is a Shulker box
        if (isShulkerBox(block.getType())) {
            String playerUUID = event.getPlayer().getUniqueId().toString();
            String coordinates = block.getLocation().getBlockX() + ","
                    + block.getLocation().getBlockY() + ","
                    + block.getLocation().getBlockZ();

            // Remove the Shulker box data from the database
            deleteShulkerBoxData(playerUUID, coordinates);
        }
    }

    private boolean isShulkerBox(final Material material) {
        return switch (material) {
            case SHULKER_BOX, WHITE_SHULKER_BOX, ORANGE_SHULKER_BOX, MAGENTA_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX,
                 YELLOW_SHULKER_BOX, LIME_SHULKER_BOX, PINK_SHULKER_BOX, GRAY_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX,
                 CYAN_SHULKER_BOX, PURPLE_SHULKER_BOX, BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, GREEN_SHULKER_BOX,
                 RED_SHULKER_BOX, BLACK_SHULKER_BOX -> true;
            default -> false;
        };
    }

    private void deleteShulkerBoxData(final String playerUUID, final String coordinates) {
        String query = "DELETE FROM items WHERE UUID = ? AND coordinates = ? AND container = 'SHULKER_BOX'";

        try (Connection connection = new DatabaseConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID);
            statement.setString(2, coordinates);
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Failed to delete Shulker box data from the database");
            e.printStackTrace();
        }
    }
}
