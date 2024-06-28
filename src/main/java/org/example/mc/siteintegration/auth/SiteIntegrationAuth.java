package org.example.mc.siteintegration.auth;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.mc.siteintegration.managers.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SiteIntegrationAuth extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("siteIntegrationAuth has been enabled!");

        // Подключение к базе данных
        try {
            DatabaseManager.connect("localhost", 3306, "root", "root", "root");
            getLogger().info("Connected to the database!");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Could not connect to the database.");
        }
    }

    @Override
    public void onDisable() {
        DatabaseManager.close();
        getLogger().info("siteIntegrationAuth has been disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("reg")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (args.length != 2) {
                    player.sendMessage("Usage: /reg <password> <repetPassword>");
                    return true;
                }

                String password = args[0];
                String repetPassword = args[1];

                if (!password.equals(repetPassword)) {
                    player.sendMessage("Passwords do not match!");
                    return true;
                }

                try (Connection conn = DatabaseManager.getConnection()) {
                    // Проверка, существует ли игрок
                    PreparedStatement checkUserStmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
                    checkUserStmt.setString(1, player.getName());
                    ResultSet rs = checkUserStmt.executeQuery();

                    if (rs.next()) {
                        player.sendMessage("Player already registered!");
                        return true;
                    }

                    // Регистрация игрока
                    PreparedStatement insertUserStmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
                    insertUserStmt.setString(1, player.getName());
                    insertUserStmt.setString(2, password);  // Note: Store hashed password in production
                    insertUserStmt.executeUpdate();

                    player.sendMessage("Player registered successfully!");
                } catch (SQLException e) {
                    e.printStackTrace();
                    player.sendMessage("An error occurred while registering. Please try again.");
                }
            } else {
                sender.sendMessage("This command can only be run by a player.");
            }
            return true;
        }
        return false;
    }
}
