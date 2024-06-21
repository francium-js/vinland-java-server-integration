package org.example.mc.siteintegration.money;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.example.mc.siteintegration.utils.PlayerError;
import org.example.mc.siteintegration.utils.PlayerMessageUtil;
import org.json.simple.JSONObject;

public class PostMoney implements CommandExecutor {

    private Player player;
    private Integer howMuchPlayerWant;
    private ItemStack shulkerWithDiamonOre;
    private BlockStateMeta shulkerMeta;
    private PlayerMessageUtil messageUtil;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Ця команда може бути активована тільки гравцем");
                return false;
            }

            this.player = (Player) sender;
            this.messageUtil = new PlayerMessageUtil(player);
            this.howMuchPlayerWant = Integer.parseInt(args[0]);
            this.shulkerWithDiamonOre = player.getInventory().getItemInMainHand();

            inspectShulkerInHande();
            inspectShulker();

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                fetchPostMoney(httpClient);
            }

            shulkerWithDiamonOre.setItemMeta(shulkerMeta);
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


    private void fetchPostMoney(CloseableHttpClient httpClient) throws Exception{
        messageUtil.toActionBar("&eТриває операція");

        String url = "http://localhost:8080/mc/user_inventory/money";
        HttpPost request = new HttpPost(url);

        JSONObject payload = new JSONObject();
        payload.put("realname", player.getName());
        payload.put("money", howMuchPlayerWant);

        Gson gson = new Gson();
        String jsonPayload = gson.toJson(payload);

        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonPayload, "UTF-8"));

            HttpResponse response = httpClient.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            switch (statusCode) {
                case 201: break;
                case 404: throw new PlayerError("&cГравця з таким ніком не знайдено");
                case 500: throw new PlayerError("&cВнутрішня помилка сервера");
                default: throw new PlayerError("&cОперація не успішна :(");
            }

            String countLittleMoneyInString = "&b" + howMuchPlayerWant + " шт. ⟡";
            String countMoneyInString = howMuchPlayerWant < 64 ? countLittleMoneyInString : "&b" + (howMuchPlayerWant - howMuchPlayerWant % 64) / 64 + "ст. i " + howMuchPlayerWant % 64 + "шт. ⟡";


            messageUtil.toActionBar("&aУспішна операція !");

            player.sendMessage("");
            messageUtil.toChat("Ви перекинули " + countMoneyInString);
            player.sendMessage("");
    }

    private void inspectShulkerInHande () throws Exception{
        shulkerMeta = (BlockStateMeta) shulkerWithDiamonOre.getItemMeta();

        if (shulkerMeta == null || !(shulkerMeta.getBlockState() instanceof ShulkerBox)) {
            throw new PlayerError("&cВ руках повинен бути ваш гаманець");
        }

        String shulkerboxName = shulkerMeta.getDisplayName();
        String skulherboxInHand = "Гаманець " + player.getName();
        boolean isIdentical = shulkerboxName.equals(skulherboxInHand);

        if (!isIdentical) throw new PlayerError("&cВ руках повинен бути ваш гаманець");
    }

    private void inspectShulker() throws Exception {
        ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();
        ItemStack[] contentsArray = shulkerBox.getInventory().getContents();

        int moneyInShulker = 0;
        int howMuchNeed = howMuchPlayerWant;

        if(howMuchNeed % 64 > 0){
            for (int i = 0; i < contentsArray.length; i++) {
                ItemStack itemSlot = contentsArray[i];

                if (itemSlot != null) {
                    ItemMeta itemMeta = itemSlot.getItemMeta();

                    if (itemMeta != null) {
                        String item_id = itemSlot.getType().name();

                        if (item_id.contains("DEEPSLATE_DIAMOND_ORE")) {
                            Integer amount = itemSlot.getAmount();

                            if((howMuchNeed % 64) == amount) {
                                howMuchNeed -= (howMuchNeed % 64);
                                itemSlot.setAmount(0);

                                moneyInShulker += amount;

                                break;
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < contentsArray.length; i++) {
            ItemStack itemSlot = contentsArray[i];

            if (itemSlot != null) {
                ItemMeta itemMeta = itemSlot.getItemMeta();

                if (itemMeta != null) {
                    String item_id = itemSlot.getType().name();

                    if (item_id.contains("DEEPSLATE_DIAMOND_ORE")) {
                        Integer amount = itemSlot.getAmount();

                        if(howMuchNeed >= 64 && amount == 64) {
                            howMuchNeed -= 64;
                            itemSlot.setAmount(0);

                            moneyInShulker += 64;
                            continue;
                        }

                        if(howMuchNeed > amount) {
                            howMuchNeed -= amount;
                            itemSlot.setAmount(0);

                            moneyInShulker += amount;
                            continue;
                        }

                        int result = amount - howMuchNeed;
                        itemSlot.setAmount(result);
                        howMuchNeed = 0;

                        moneyInShulker += (amount - result);
                    }
                }
            }
        }

        if(moneyInShulker < howMuchPlayerWant) throw new PlayerError("&cУ вас недостатньо валюти");
        if(moneyInShulker == 0) throw new PlayerError("&cВ гаманці не знайдена валюта");

        shulkerBox.getInventory().setContents(contentsArray);
        shulkerMeta.setBlockState(shulkerBox);
    }
}