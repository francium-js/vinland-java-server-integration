package org.example.mc.siteintegration.managers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;

public class DatabaseManager {
    private static Connection connection;

    public static CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection("jdbc:mysql://db.majorcore.com:3306/s763_mcSiteIntegration", "u763_yWyPUzAWaz", "=r2Cu3vB=WyJ3Ge95I6mtEj7");
                Bukkit.getLogger().info("connection");
            } catch (ClassNotFoundException | SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                throw new SQLException("Database connection is not available.");
            }
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();  // Логгируйте или обрабатывайте исключение здесь
            return null;  // Или выполните другую логику в случае ошибки подключения
        }
    }

    public static synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}