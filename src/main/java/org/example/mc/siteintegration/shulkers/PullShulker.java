package org.example.mc.siteintegration.shulkers;

import java.util.concurrent.CompletableFuture;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.example.mc.siteintegration.helpers.ItemSerializer;
import org.example.mc.siteintegration.utils.PlayerError;
import org.example.mc.siteintegration.utils.PlayerMessageUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;

public class PullShulker implements CommandExecutor {

    private Player player;
    private ItemStack itemInHand;
    private Integer shulkerId;
    private BlockStateMeta shulkerMeta;
    private PlayerMessageUtil messageUtil;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try{
            if (!(sender instanceof Player)) {
                sender.sendMessage("Ця команда може бути активована тільки гравцем");
                return false;
            }

            this.player = (Player) sender;
            this.messageUtil = new PlayerMessageUtil(player);
            this.shulkerId = Integer.parseInt(args[0]);
            this.itemInHand = player.getInventory().getItemInMainHand();

            inspectItemInHand();
            fetchPullShulker();
        } catch (PlayerError e){
            messageUtil.toActionBar(e.getMessage());
            return false;
        } catch (Exception e){
            Bukkit.getLogger().warning(e.getMessage());
            messageUtil.toChat("Помилка в плагіні, будь ласка повідомте адмінів");
            return false;
        }

        return true;
    }

    private void containNewItemToShulker(JSONArray serializedArray) throws Exception{
        ItemStack[] itemStackItems = new ItemStack[serializedArray.size()];

        for (int i = 0; i < serializedArray.size(); i++) {
            itemStackItems[i] = ItemSerializer.deserializeItem((String) serializedArray.get(i));
        }

        ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();
        ItemStack[] contentsArray = shulkerBox.getInventory().getContents();

        for (int i = 0, y = 0; i < contentsArray.length; i++) {
            ItemStack itemSlot = contentsArray[i];

            if(itemSlot == null){
                contentsArray[i] = itemStackItems[y];
                y+= 1;

                if (y == serializedArray.size()) break;
            }
        }

        shulkerBox.getInventory().setContents(contentsArray);
        shulkerMeta.setBlockState(shulkerBox);
    }

    private void fetchDeleteItems() throws Exception  {
        String url = "https://mc-back-end.onrender.com/mc/user/shulker/" + shulkerId + '/' + player.getName();
        HttpDelete request = new HttpDelete(url);

        CompletableFuture.runAsync(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) { 

            HttpResponse response = httpClient.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            String responseBody = EntityUtils.toString(response.getEntity());

            JSONParser parser = new JSONParser();
            JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);

            if(statusCode >= 300 && statusCode < 600) {
                String errorMessage = jsonResponse.get("message").toString();

                throw new PlayerError("&c" + errorMessage);
            }
        } catch (PlayerError e){
            messageUtil.toChat(e.getMessage());
        } catch(Exception e){
            throw new Error(e);
        }
    });
    }

    private void fetchPullShulker() throws Exception  {
        messageUtil.toActionBar("&eТриває операція");

        String url = "https://mc-back-end.onrender.com/mc/user/shulker";
        HttpPut request = new HttpPut(url);

        JSONObject payload = new JSONObject();
        payload.put("username", player.getName());
        payload.put("shulkerId", shulkerId);

        Gson gson = new Gson();
        String jsonPayload = gson.toJson(payload);

        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonPayload, "UTF-8"));

        CompletableFuture.runAsync(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) { 

            HttpResponse response = httpClient.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            String responseBody = EntityUtils.toString(response.getEntity());

            JSONParser parser = new JSONParser();
            JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);

            if(statusCode >= 300 && statusCode < 600) {
                String errorMessage = jsonResponse.get("message").toString();

                throw new PlayerError("&c" + errorMessage);
            }

            inspectItemInHand();

            JSONArray serializedArray = (JSONArray) jsonResponse.get("shulkerItemsData");
            String shulkerType = jsonResponse.get("shulkerType").toString().toUpperCase();
            String shulkerName = jsonResponse.get("shulkerName").toString();

            Material material = Material.valueOf(shulkerType);

            ItemStack shulkerBoxItem = new ItemStack(material);

            containNewItemToShulker(serializedArray);

            shulkerMeta.setDisplayName(shulkerName);

            shulkerBoxItem.setItemMeta(shulkerMeta);
            player.getInventory().setItemInMainHand(shulkerBoxItem);
            
            messageUtil.toActionBar("&aВи успішно забрали предмети");

            fetchDeleteItems();
        } catch (PlayerError e){
            messageUtil.toChat(e.getMessage());
        } catch(Exception e){
            throw new Error(e);
        }
    });
    }

    private void inspectItemInHand() throws Exception{
        if (!player.isOnline()) throw new Error("&cГравець вийшов з гри.");

        if (itemInHand != null || itemInHand.getType() != Material.AIR) {
            throw new PlayerError("&cВ руках нічого не повинно бути");
        }
    }
}