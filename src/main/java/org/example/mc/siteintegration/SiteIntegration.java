package org.example.mc.siteintegration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.mc.siteintegration.commands.TradeCommandExecutor;
import org.example.mc.siteintegration.commands.TradeTabCompleter;
import org.json.simple.JSONObject;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class SiteIntegration extends JavaPlugin {

    private final Gson gson = new Gson();
    private Logger logger;

    @Override
    public void onEnable() {
        logger = getLogger();
        getLogger().info("ShulkerInspectPlugin has been enabled!");

        TradeCommandExecutor tradeExecutor = new TradeCommandExecutor();


        getCommand("trade").setExecutor(tradeExecutor);
        getCommand("trade").setTabCompleter(new TradeTabCompleter());

        new BukkitRunnable() {
            @Override
            public void run() {
                processUserCache();
            }
        }.runTaskAsynchronously(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("SiteIntegration has been disabled.");
    }

    private void processUserCache() {
        Path userCachePath = getServer().getWorldContainer().toPath().resolve("usercache.json");

        if (Files.exists(userCachePath)) {
            try (FileReader reader = new FileReader(userCachePath.toFile())) {
                Type userListType = new TypeToken<List<User>>() {}.getType();
                List<User> users = gson.fromJson(reader, userListType);

                if (users == null || users.isEmpty()) logger.warning("No user data found in the user cache file.");
                readUserAdvancements(users);

            } catch (IOException e) {
                logger.severe("Error reading user cache file: " + e.getMessage());
            }
        } else {
            logger.warning("User cache file not found.");
        }
    }

    private void readUserAdvancements(List<User> users) {
        Path advancementsPath = getServer().getWorldContainer().toPath().resolve("world/advancements");

        for (User user : users) {
            Path userAdvancementFile = advancementsPath.resolve(user.uuid + ".json");

            if (Files.exists(userAdvancementFile)) {
                try (FileReader reader = new FileReader(userAdvancementFile.toFile())) {
                    Object advancementsData = gson.fromJson(reader, Object.class);
                    fetchPutAdvancements(user.name,advancementsData);
                } catch (IOException e) {
                    logger.severe("Error reading advancements file for user " + user.name + ": " + e.getMessage());
                }
            } else {
                logger.warning("Advancements file not found for user " + user.name + " with UUID " + user.uuid);
            }
        }
    }
    private static class User {
        String name;
        String uuid;
        String expiresOn;
    }

    private void fetchPutAdvancements(String username, Object advancements) {
    CompletableFuture.runAsync(() -> {
        HttpClient httpClient = HttpClients.createDefault();
        String url = "http://localhost:8080/mc/user/advancements";
        HttpPut request = new HttpPut(url);

        try {
            JSONObject payload = new JSONObject();
            payload.put("username", username);
            payload.put("data", advancements);

            Gson gson = new Gson();
            String jsonPayload = gson.toJson(payload);

            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonPayload, "UTF-8"));

            httpClient.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
}

}
