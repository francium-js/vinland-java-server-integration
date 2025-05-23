package org.example.mc.siteintegration.money;

import com.google.gson.Gson;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.example.mc.siteintegration.constants.UrlConstants;
import org.example.mc.siteintegration.utils.PlayerError;
import org.example.mc.siteintegration.utils.PlayerMessageUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class PullMoney implements CommandExecutor {

    private Player player;
    private ItemStack shulkerWithDiamonOre;
    private Integer howMuchWantMoney;
    private BlockStateMeta shulkerMeta;
    private PlayerMessageUtil messageUtil;
    private String cacheId = UUID.randomUUID().toString();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try{
            if (!(sender instanceof Player)) {
                sender.sendMessage("Ця команда може бути активована тільки гравцем");
                return false;
            }

            this.player = (Player) sender;
            this.messageUtil = new PlayerMessageUtil(player);
            this.howMuchWantMoney = Integer.parseInt(args[0]);
            this.shulkerWithDiamonOre = player.getInventory().getItemInMainHand();

            inspectShulkerInHand();
            fetchGetMoneyCount();
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

    private void inspectShulkerInHand() throws Exception{
        shulkerMeta = (BlockStateMeta) shulkerWithDiamonOre.getItemMeta();

        if (shulkerMeta == null || !(shulkerMeta.getBlockState() instanceof ShulkerBox)) {
            throw new PlayerError("&cВ руках повинен бути ваш гаманець");
        }

        String shulkerboxName = shulkerMeta.getDisplayName();
        boolean isIdenticalWalletName = shulkerboxName.toLowerCase().contains("Гаманець".toLowerCase());
        boolean isIdenticalPlayerNameInWallet = shulkerboxName.contains(player.getName());

        if (!isIdenticalWalletName || !isIdenticalPlayerNameInWallet) throw new PlayerError("&cВ руках повинен бути ваш гаманець");
    }

    private void fetchGetMoneyCount() throws Exception {
        String url = UrlConstants.BACKEND_URL + "mc/user/money/" + player.getName();
        HttpGet request = new HttpGet(url);

        CompletableFuture.runAsync(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) { 
                HttpResponse response = httpClient.execute(request);

                int statusCode = response.getStatusLine().getStatusCode();
                int moneyInAuctionInventory = 0;

                String responseBody = EntityUtils.toString(response.getEntity());

                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);

                if(statusCode >= 300 && statusCode < 600) {
                    String errorMessage = jsonResponse.get("message").toString();
    
                    throw new PlayerError("&c" + errorMessage);
                }

                moneyInAuctionInventory = Integer.parseInt(jsonResponse.get("money").toString());

                if(moneyInAuctionInventory < howMuchWantMoney) throw new PlayerError("&cУ вас немає такої кількості валюти");

                inspectShulkerContents();
            } catch (PlayerError e){
                messageUtil.toChat(e.getMessage());
            } catch(Exception e){
                throw new Error(e);
            }
        });
    }

    private void inspectShulkerContents() throws Exception{
        ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();
        ItemStack[] contentsArray = shulkerBox.getInventory().getContents();

        Integer howMuchPlaceMoney = 0;
        Integer localHowMuchWantMoney = howMuchWantMoney;

        for (int i = 0; i < contentsArray.length; i++) {
            ItemStack itemSlot = contentsArray[i];

            if (itemSlot != null && itemSlot.getType() == Material.DEEPSLATE_DIAMOND_ORE) {
                int amount = itemSlot.getAmount();

                if (amount == 64) continue;

                int sum = amount + localHowMuchWantMoney;
                if (sum <= 64) {
                    itemSlot.setAmount(sum);
                    localHowMuchWantMoney = 0;
                    howMuchPlaceMoney += 64;
                } else {
                    itemSlot.setAmount(64);
                    localHowMuchWantMoney -= (64 - amount);
                    howMuchPlaceMoney += amount;
                }

                contentsArray[i] = itemSlot;
            } else if (itemSlot == null) {
                ItemStack diamondOreStack = new ItemStack(Material.DEEPSLATE_DIAMOND_ORE);
                ItemMeta diamondOreMeta = diamondOreStack.getItemMeta();
                diamondOreStack.setItemMeta(diamondOreMeta);

                if (localHowMuchWantMoney >= 64) {
                    diamondOreStack.setAmount(64);
                    localHowMuchWantMoney -= 64;
                    howMuchPlaceMoney += 64;
                } else {
                    diamondOreStack.setAmount(localHowMuchWantMoney);

                    howMuchPlaceMoney += localHowMuchWantMoney;
                    localHowMuchWantMoney = 0;
                }
                contentsArray[i] = diamondOreStack;
            }
        }

        if(localHowMuchWantMoney < 0) throw new PlayerError("&cВ гаманці немає місця");
        if (howMuchPlaceMoney == 0) throw new PlayerError("&cВ гаманці немає місця");

        shulkerBox.getInventory().setContents(contentsArray);
        this.shulkerMeta.setBlockState(shulkerBox);

        fetchPutMoney();
    }

    private void fetchPutMoney() throws Exception{
        messageUtil.toActionBar("&eТриває операція");

        String url = UrlConstants.BACKEND_URL + "mc/user/money";
        HttpPut request = new HttpPut(url);

        JSONObject payload = new JSONObject();
        
        payload.put("username", player.getName());
        payload.put("money", howMuchWantMoney);
        payload.put("cacheId", cacheId);

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

                if (!player.isOnline()) throw new Error("&cГравець вийшов з гри.");

                ItemStack currentItemInMainHand = player.getInventory().getItemInMainHand();
                if (!currentItemInMainHand.equals(shulkerWithDiamonOre)) {
                    throw new PlayerError("&cВи більше не тримаєте ваш гаманець.");
                }
    
                int moneyBefore = Integer.parseInt(jsonResponse.get("moneyBefore").toString());
                int moneyAfter = Integer.parseInt(jsonResponse.get("moneyAfter").toString());
    
                String countLittleMoneyInString = "&b" + howMuchWantMoney + "шт. ⟡";
                String moneyCountInStack = "&b" + howMuchWantMoney / 64 + "ст. ⟡";
    
                String countMoneyInString = howMuchWantMoney % 64 == 0 ? moneyCountInStack : howMuchWantMoney < 64 ? countLittleMoneyInString : "&b" + (howMuchWantMoney - howMuchWantMoney % 64) / 64 + "ст. i " + howMuchWantMoney % 64 + "шт. ⟡";
    
                messageUtil.toActionBar("&aУспішна операція !");
    
                player.sendMessage("");
                messageUtil.toChat("&7Ви зняли &b" + countMoneyInString);
                messageUtil.toChat("&b" + moneyBefore + "⟡ &7---> " + "&b" + moneyAfter + "⟡");
                player.sendMessage("");

                shulkerWithDiamonOre.setItemMeta(shulkerMeta);

                fetchPutMoneyConfirm();
            } catch (PlayerError e){
                messageUtil.toChat(e.getMessage());
            } catch(Exception e){
                throw new Error(e);
            }
        });
    }

    private void fetchPutMoneyConfirm() throws Exception{
        String url = UrlConstants.BACKEND_URL + "mc/user/money/confirm";
        HttpPut request = new HttpPut(url);

        JSONObject payload = new JSONObject();
    
        payload.put("cacheId", cacheId);

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
            } catch (PlayerError e){
                messageUtil.toChat(e.getMessage());
            } catch(Exception e){
                throw new Error(e);
            }
        });
    }
}