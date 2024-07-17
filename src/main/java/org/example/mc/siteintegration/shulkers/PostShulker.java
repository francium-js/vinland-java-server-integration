package org.example.mc.siteintegration.shulkers;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.example.mc.siteintegration.constants.UrlConstants;
import org.example.mc.siteintegration.entities.ShulkerData;
import org.example.mc.siteintegration.helpers.ItemSerializer;
import org.example.mc.siteintegration.utils.PlayerError;
import org.example.mc.siteintegration.utils.PlayerMessageUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PostShulker implements CommandExecutor {

    private Player player;
    private ItemStack shulkerBoxInMainHand;
    private JSONArray itemsInShulker;
    private BlockStateMeta shulkerMeta;
    private PlayerMessageUtil messageUtil;
    private ShulkerData shulkerData;
    private String cacheId = UUID.randomUUID().toString();


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try{
            if (!(sender instanceof Player)) {
                sender.sendMessage("Ця команда може бути активована тільки гравцем");
                return true;
            }

            this.player = (Player) sender;
            this.messageUtil = new PlayerMessageUtil(player);
            this.shulkerBoxInMainHand = player.getInventory().getItemInMainHand();

            inspectShulkerInHand();
            inspectShulker();
            
            fetchPostShulker();
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
    
    private void fetchPostShulkerConfirm () throws Exception {

        String url = UrlConstants.BACKEND_URL + "mc/user/shulkers/confirm";
        HttpPost request = new HttpPost(url);

        JSONObject payload = new JSONObject();

        payload.put("cacheId", cacheId);
        payload.put("username", player.getName());

        Gson gson = new Gson();
        String jsonPayload = gson.toJson(payload);

        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonPayload, "UTF-8"));

        CompletableFuture.runAsync(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) { 
        
                HttpResponse response = httpClient.execute(request);

                int statusCode = response.getStatusLine().getStatusCode();

                if(statusCode >= 300 && statusCode < 600) {
                
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);

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

    private void fetchPostShulker () throws Exception {
        messageUtil.toActionBar("&eТриває операція");

        String url = UrlConstants.BACKEND_URL + "mc/user/shulkers";
        HttpPost request = new HttpPost(url);

        JSONObject payload = new JSONObject();

        payload.put("username", player.getName());
        payload.put("itemsData", itemsInShulker);
        payload.put("cacheId", cacheId);
        payload.put("shulkerData", shulkerData);

        Gson gson = new Gson();
        String jsonPayload = gson.toJson(payload);

        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonPayload, "UTF-8"));

        CompletableFuture.runAsync(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) { 
        
                HttpResponse response = httpClient.execute(request);

                int statusCode = response.getStatusLine().getStatusCode();

                if(statusCode >= 300 && statusCode < 600) {
                
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);

                String errorMessage = jsonResponse.get("message").toString();

                throw new PlayerError("&c" + errorMessage);
                }

                if (!player.isOnline()) throw new Error("&cГравець вийшов з гри.");

                ItemStack currentItemInMainHand = player.getInventory().getItemInMainHand();
                if (!currentItemInMainHand.equals(shulkerBoxInMainHand)) {
                    throw new PlayerError("&cВи більше не тримаєте шалкер у руці.");
                }

                messageUtil.toActionBar("&aУспішна операція !");

                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

                fetchPostShulkerConfirm();
            } catch (PlayerError e){
                messageUtil.toChat(e.getMessage());
            } catch(Exception e){
                throw new Error(e);
            }
        });
    }


    private void inspectShulker() throws Exception{
        JSONArray itemsArray = new JSONArray();
        ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();
        ItemStack[] contentsArray = shulkerBox.getInventory().getContents();

        Pattern pattern = Pattern.compile("\\[minecraft:(\\w+)]");

        for (int i = 0; i < contentsArray.length; i++) {
            ItemStack itemSlot = contentsArray[i];

            if (itemSlot != null) {
                ItemMeta itemMeta = itemSlot.getItemMeta();

                if (itemMeta != null) {
                    String item_id = itemSlot.getType().name();

                    if (item_id.contains("DEEPSLATE_DIAMOND_ORE")) throw new PlayerError("&cВ шалкері не може бути валюта");

                    if (itemMeta instanceof PotionMeta) continue;

                    JSONObject itemObject = new JSONObject();

                    itemObject.put("serialized", ItemSerializer.serializeItem(contentsArray[i]));
                    itemObject.put("type", itemSlot.getType().toString().toLowerCase());
                    itemObject.put("amount", itemSlot.getAmount());
                    itemObject.put("display_name", itemMeta.getDisplayName());

                    JSONArray result = new JSONArray();


                    if (!(itemMeta.getEnchants().entrySet().isEmpty())) {
                        for (Map.Entry<Enchantment, Integer> entry : itemMeta.getEnchants().entrySet()) {
                            String key = entry.getKey().toString();
                            Integer value = entry.getValue();

                            Matcher matcher = pattern.matcher(key);
                            if (matcher.find()) {
                                String enchantName = matcher.group(1);

                                result.add(enchantName + "$" + value);
                            }
                        }

                        if (!result.isEmpty()) itemObject.put("enchants", result);
                    }

                    if (itemMeta instanceof EnchantmentStorageMeta) {
                        EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta) itemMeta;

                        for (Map.Entry<Enchantment, Integer> entry : enchantMeta.getStoredEnchants().entrySet()) {

                            Matcher matcher = pattern.matcher(entry.getKey().toString());
                            if (matcher.find()) {
                                String enchantName = matcher.group(1);

                                result.add(enchantName + "$" + entry.getValue());
                            }
                        }

                        if (!result.isEmpty()) itemObject.put("enchants", result);
                    }

                    itemSlot.setAmount(0);
                    itemsArray.add(itemObject);
                }
            }
        }
        if(itemsArray.isEmpty()) throw new PlayerError("&cШалкер пустий");

        String type = shulkerBox.getType().toString().toLowerCase();
        String display_name = shulkerMeta.getDisplayName();

        shulkerData = new ShulkerData(display_name, type);
        itemsInShulker = itemsArray;
    }

    private void inspectShulkerInHand() throws Exception{
        shulkerMeta = (BlockStateMeta) shulkerBoxInMainHand.getItemMeta();

        if (shulkerMeta == null || !(shulkerMeta.getBlockState() instanceof ShulkerBox)) {
            throw new PlayerError("&cВ руках повинен бути шалкер");
        }
    }
}