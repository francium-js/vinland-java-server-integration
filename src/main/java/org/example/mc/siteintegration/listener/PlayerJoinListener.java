package org.example.mc.siteintegration.listener;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
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

public class PlayerJoinListener implements Listener {
    private final Set<UUID> visitedPlayers = new HashSet<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!visitedPlayers.contains(uuid)) {
            visitedPlayers.add(uuid);

            Bukkit.getLogger().warning(uuid + "");

            HttpClient httpClient = HttpClients.createDefault();
            String url = "http://localhost:8080/mc/user/userUUID";
            HttpPost request = new HttpPost(url);

            JSONObject payload = new JSONObject();
            payload.put("realname", player.getName());
            payload.put("uuid", uuid);

            Gson gson = new Gson();
            String jsonPayload = gson.toJson(payload);

            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonPayload, "UTF-8"));

            try{
                httpClient.execute(request);
            }catch (Exception e){
                Bukkit.getLogger().warning(e.getMessage());
            }
        }
    }
}