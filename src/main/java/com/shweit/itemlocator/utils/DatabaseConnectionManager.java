package com.shweit.itemlocator.utils;

import com.shweit.itemlocator.ItemLocator;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.nio.charset.StandardCharsets;

public final class DatabaseConnectionManager {

    public Connection getConnection() {
        try {
            File dbFile = new File(ItemLocator.getInstance().getDataFolder(), "items.sqlite");
            return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
        } catch (Exception e) {
            Logger.error("Failed to connect to the database");
            e.printStackTrace();
            return null;
        }
    }

    public void setUpDatabase() {
        File dataFolder = ItemLocator.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs(); // Ensure the data folder is created
        }

        File dbFile = new File(dataFolder, "items.sqlite");
        if (!dbFile.exists()) {
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath())) {
                Logger.info("Database file created at: " + dbFile.getPath());
                executeSQLScript(); // Execute the SQL script directly from JAR
            } catch (Exception e) {
                Logger.error("Failed to create the database");
                e.printStackTrace();
            }
        }
    }

    private void executeSQLScript() {
        try (Connection connection = getConnection()) {
            // Load the SQL script from the resources folder inside the JAR
            InputStream in = ItemLocator.getInstance().getResource("sql/create.sql");
            if (in != null) {
                String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                connection.createStatement().execute(script);
                Logger.info("Executed SQL script from resources");
            } else {
                Logger.error("SQL script file not found in JAR: sql/create.sql");
            }
        } catch (Exception e) {
            Logger.error("Failed to execute the SQL script");
            e.printStackTrace();
        }
    }
}
