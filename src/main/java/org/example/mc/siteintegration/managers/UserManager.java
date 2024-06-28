package org.example.mc.siteintegration.managers;

import org.bukkit.Bukkit;
import org.example.mc.siteintegration.entities.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserManager {
    private final Map<String, User> userCache = new HashMap<>();

    Connection conn;

    public UserManager(Connection conn) {
        this.conn = conn;
    }

    public CompletableFuture<Void> loadUsers() {
        return CompletableFuture.runAsync(() -> {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT username, password, uuid, ip, mcSession FROM users");

                while (rs.next()) {
                    String username = rs.getString("username");
                    String passwordHash = rs.getString("password");
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String ip = rs.getString("ip");
                    Instant mcSession = Instant.ofEpochMilli(rs.getLong("mcSession"));

                    User user = new User(username, passwordHash, uuid, ip, mcSession);
                    userCache.put(username, user);
                }
            } catch (SQLException e) {
                Bukkit.getLogger().warning(e.toString());
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> registerUser(String username, String password, UUID uuid, String ip) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        Instant instant = Instant.now().plusSeconds(10); // 7 дней

        ZoneId kievZone = ZoneId.of("Europe/Kiev");
        Instant mcSession = instant.atZone(kievZone).toInstant();


        User user = new User(username, hashedPassword, uuid, ip, mcSession);
        userCache.put(username, user);

        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password, uuid, ip, mcSession) VALUES (?, ?, ?, ?, ?)");
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                stmt.setString(3, uuid.toString());
                stmt.setString(4, ip);
                stmt.setLong(5, mcSession.toEpochMilli());
                stmt.executeUpdate();
            } catch (SQLException e) {
                Bukkit.getLogger().warning(e.toString());
                e.printStackTrace();
            }
        });
    }

    public Boolean validatePassword(String username, String password) {
        User user = getUser(username);
            if (user != null) {
                return BCrypt.checkpw(password, user.getPasswordHash());
            }
            return false;
    }

    public CompletableFuture<Void> updateSession(String username, String ip) {
        User user = getUser(username);

        return CompletableFuture.runAsync(() -> {
            if (user != null) {
                Instant instant = Instant.now().plusSeconds(10); // 7 дней

                ZoneId kievZone = ZoneId.of("Europe/Kiev");
                Instant mcSession = instant.atZone(kievZone).toInstant();

                userCache.put(username, user);

                try {
                    PreparedStatement stmt = conn.prepareStatement("UPDATE users SET mcSession = ?, ip = ? WHERE username = ?");
                    stmt.setLong(1, mcSession.toEpochMilli());
                    stmt.setString(2, ip);
                    stmt.setString(3, username);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    Bukkit.getLogger().warning(e.toString());
                    e.printStackTrace();
                }
            }
        });
    }

    public User getUser(String username) {
        return userCache.get(username);
    }
}
