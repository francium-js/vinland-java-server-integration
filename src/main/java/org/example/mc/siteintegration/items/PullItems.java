package org.example.mc.siteintegration.items;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.example.mc.siteintegration.utils.PlayerError;
import org.example.mc.siteintegration.utils.PlayerMessageUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class PullItems implements CommandExecutor {

    private Player player;
    private ItemStack shulkerBoxInMainHand;
    private Integer itemTicketId;
    private Integer countEmptySlots = 0;
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
            this.itemTicketId = Integer.parseInt(args[0]);;
            this.shulkerBoxInMainHand = player.getInventory().getItemInMainHand();

            inspectShulkerInHand();
            inspectShulkerContents();
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                fetchGetItemTicketInfo(httpClient);
                fetchPullItems(httpClient);
            }

            shulkerBoxInMainHand.setItemMeta(shulkerMeta);
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

    private void fetchGetItemTicketInfo(CloseableHttpClient httpClient) throws Exception{
        messageUtil.toActionBar("&eТриває операція");

        String url = "http://localhost:8080/mc/item_ticket/countSlots?realname="+ player.getName() + "&itemTicketId=" + itemTicketId;
        HttpGet request = new HttpGet(url);

        Integer needCountSlots;

            HttpResponse response = httpClient.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            switch (statusCode) {
                case 200: break;
                case 404: throw new PlayerError("&cКвиток з таким id не існує");
                case 500: throw new PlayerError("&cВнутрішня помилка сервера");
                default: throw new PlayerError("&cОперація не успішна :(");
            }

            String responseBody = EntityUtils.toString(response.getEntity());

            JSONParser parser = new JSONParser();
            JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);

            needCountSlots = Integer.parseInt(jsonResponse.get("countSlots").toString());

            if(countEmptySlots < needCountSlots) throw new PlayerError("&cВ шалкері не вистачає місця");
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

    private void fetchPullItems(CloseableHttpClient httpClient) throws Exception  {
        messageUtil.toActionBar("&eТриває операція");

        String url = "http://localhost:8080/mc/user_inventory/items/" + itemTicketId;
        HttpPut request = new HttpPut(url);

            HttpResponse response = httpClient.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            switch (statusCode) {
                case 201: break;
                case 404: throw new PlayerError("&cГравець не знайдений");
                case 500: throw new PlayerError("&cВнутрішня помилка сервера");
                default: throw new PlayerError("&cОперація не успішна :(");
            }

            String responseBody = EntityUtils.toString(response.getEntity());
            JSONParser parser = new JSONParser();
            JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);

            JSONArray serializedArray = (JSONArray) jsonResponse.get("data");
            containNewItemToShulker(serializedArray);

            messageUtil.toActionBar("&aВи успішно забрали предмети");
    }

    private void inspectShulkerContents() {
        ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();

        ItemStack[] contentsArray = shulkerBox.getInventory().getContents();

        for (int i = 0; i < contentsArray.length; i++) {
            ItemStack itemSlot = contentsArray[i];

            if (itemSlot == null) countEmptySlots+= 1;
        }
    }

    private void inspectShulkerInHand() throws Exception{
        this.shulkerMeta = (BlockStateMeta) player.getInventory().getItemInMainHand().getItemMeta();

        if (shulkerMeta == null || !(shulkerMeta.getBlockState() instanceof ShulkerBox)) {
            throw new PlayerError("&cВ руках повинен бути шалкер");
        }
    }
}