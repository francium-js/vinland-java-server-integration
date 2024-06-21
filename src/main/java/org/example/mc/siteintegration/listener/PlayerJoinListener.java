package org.example.mc.siteintegration.listener;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerJoinListener implements Listener {
    private final Set<UUID> visitedPlayers = new HashSet<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!visitedPlayers.contains(uuid)) {
            visitedPlayers.add(uuid);
            Bukkit.getLogger().info("Player UUID: " + uuid);

            CompletableFuture.runAsync(() -> {
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    String url = "http://localhost:8080/mc/user/userUUID";
                    HttpPost request = new HttpPost(url);

                    JSONObject payload = new JSONObject();
                    payload.put("realname", player.getName());
                    payload.put("uuid", uuid.toString());

                    Gson gson = new Gson();
                    String jsonPayload = gson.toJson(payload);

                    request.setHeader("Content-Type", "application/json");
                    request.setEntity(new StringEntity(jsonPayload, "UTF-8"));

                    HttpResponse response = httpClient.execute(request);
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == 200) {
                        Bukkit.getLogger().info("Successfully sent player data for " + player.getName());
                    } else {
                        Bukkit.getLogger().warning("Failed to send player data for " + player.getName() + " - Status code: " + statusCode);
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Error sending player data: " + e.getMessage());
                }
            });
        }
    }
}